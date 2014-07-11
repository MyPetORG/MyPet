/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
 *
 * MyPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.entity.ai.attack;

import de.Keyle.MyPet.entity.ai.AIGoal;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.skill.skills.Skills;
import de.Keyle.MyPet.skill.skills.implementation.Ranged;
import de.Keyle.MyPet.skill.skills.implementation.ranged.*;
import de.Keyle.MyPet.skill.skills.info.RangedInfo.Projectiles;
import net.minecraft.server.v1_7_R4.EntityArrow;
import net.minecraft.server.v1_7_R4.EntityLiving;
import net.minecraft.server.v1_7_R4.MathHelper;
import net.minecraft.server.v1_7_R4.World;

public class RangedAttack extends AIGoal {
    private MyPet myPet;
    private final EntityMyPet entityMyPet;
    private EntityLiving target;
    private int shootTimer;
    private float walkSpeedModifier;
    private int lastSeenTimer;
    private float range;
    private Ranged rangedSkill;

    public RangedAttack(EntityMyPet entityMyPet, float walkSpeedModifier, float range) {
        this.entityMyPet = entityMyPet;
        this.myPet = entityMyPet.getMyPet();
        this.shootTimer = -1;
        this.lastSeenTimer = 0;
        this.walkSpeedModifier = walkSpeedModifier;
        this.range = range;
        rangedSkill = entityMyPet.getMyPet().getSkills().getSkill(Ranged.class);
    }

    @Override
    public boolean shouldStart() {
        if (myPet.getRangedDamage() <= 0) {
            return false;
        }
        EntityLiving goalTarget = this.entityMyPet.getGoalTarget();

        if (goalTarget == null || !goalTarget.isAlive() || !entityMyPet.canMove()) {
            return false;
        }
        double meleeDamage = myPet.getDamage();
        if (meleeDamage > 0 && this.entityMyPet.f(goalTarget.locX, goalTarget.boundingBox.b, goalTarget.locZ) < 4) {
            if (meleeDamage > rangedSkill.getDamage()) {
                return false;
            }
        }
        this.target = goalTarget;
        return true;
    }

    @Override
    public boolean shouldFinish() {
        if (entityMyPet.getGoalTarget() == null || !target.isAlive() || myPet.getRangedDamage() <= 0 || !entityMyPet.canMove()) {
            return true;
        }
        double meleeDamage = myPet.getDamage();
        if (meleeDamage > 0 && this.entityMyPet.f(target.locX, target.boundingBox.b, target.locZ) < 4) {
            if (meleeDamage > rangedSkill.getDamage()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void finish() {
        this.target = null;
        this.lastSeenTimer = 0;
        this.shootTimer = -1;

        this.entityMyPet.petNavigation.getParameters().removeSpeedModifier("RangedAttack");
    }

    @Override
    public void tick() {
        double distanceToTarget = this.entityMyPet.f(this.target.locX, this.target.boundingBox.b, this.target.locZ);
        boolean canSee = this.entityMyPet.getEntitySenses().canSee(this.target);

        if (canSee) {
            this.lastSeenTimer++;
        } else {
            this.lastSeenTimer = 0;
        }

        if ((distanceToTarget <= this.range) && (this.lastSeenTimer >= 20)) {
            this.entityMyPet.petNavigation.getParameters().removeSpeedModifier("RangedAttack");
            this.entityMyPet.petNavigation.stop();
        } else {
            this.entityMyPet.petNavigation.getParameters().addSpeedModifier("RangedAttack", walkSpeedModifier);
            this.entityMyPet.petNavigation.navigateTo(this.target);
        }

        this.entityMyPet.getControllerLook().a(this.target, 30.0F, 30.0F);

        if (--this.shootTimer <= 0) {
            if (distanceToTarget < this.range && canSee) {
                shootProjectile(this.target, (float) myPet.getRangedDamage(), getProjectile());
                this.shootTimer = this.rangedSkill.getRateOfFire();
            }
        }
    }

    private Projectiles getProjectile() {
        Skills skills = entityMyPet.getMyPet().getSkills();
        if (skills.isSkillActive(Ranged.class)) {
            return skills.getSkill(Ranged.class).getProjectile();
        }
        return Projectiles.Arrow;
    }

    public void shootProjectile(EntityLiving target, float damage, Projectiles projectile) {
        World world = target.world;

        if (projectile == Projectiles.Arrow) {
            EntityArrow arrow = new MyPetArrow(world, entityMyPet, target, 1.6F, 1);
            arrow.b(damage);
            arrow.setCritical(false);
            entityMyPet.makeSound("random.bow", 1.0F, 1.0F / (entityMyPet.getRandom().nextFloat() * 0.4F + 0.8F));
            world.addEntity(arrow);
        } else if (projectile == Projectiles.Snowball) {
            MyPetSnowball snowball = new MyPetSnowball(world, entityMyPet);
            double distanceX = target.locX - entityMyPet.locX;
            double distanceY = target.locY + target.getHeadHeight() - 1.100000023841858D - snowball.locY;
            double distanceZ = target.locZ - entityMyPet.locZ;
            float distance20percent = MathHelper.sqrt(distanceX * distanceX + distanceZ * distanceZ) * 0.2F;
            snowball.setDamage(damage);
            snowball.shoot(distanceX, distanceY + distance20percent, distanceZ, 1.6F, 1);
            entityMyPet.makeSound("random.bow", 1.0F, 1.0F / (entityMyPet.getRandom().nextFloat() * 0.4F + 0.8F));
            world.addEntity(snowball);
        } else if (projectile == Projectiles.LargeFireball) {
            double distanceX = this.target.locX - entityMyPet.locX;
            double distanceY = this.target.boundingBox.b + this.target.length / 2.0F - (entityMyPet.locY + entityMyPet.length / 2.0F);
            double distanceZ = this.target.locZ - entityMyPet.locZ;
            MyPetLargeFireball largeFireball = new MyPetLargeFireball(world, entityMyPet, distanceX, distanceY, distanceZ);
            largeFireball.locY = (entityMyPet.locY + entityMyPet.length / 2.0F + 0.5D);
            largeFireball.setDamage(damage);
            world.addEntity(largeFireball);
            world.makeSound(entityMyPet.locX + 0.5D, entityMyPet.locY + 0.5D, entityMyPet.locZ + 0.5D, "mob.ghast.fireball", 1.0F + entityMyPet.getRandom().nextFloat(), entityMyPet.getRandom().nextFloat() * 0.7F + 0.3F);
        } else if (projectile == Projectiles.SmallFireball) {
            double distanceX = this.target.locX - entityMyPet.locX;
            double distanceY = this.target.boundingBox.b + this.target.length / 2.5F - (entityMyPet.locY + entityMyPet.length / 2.5F);
            double distanceZ = this.target.locZ - entityMyPet.locZ;
            MyPetSmallFireball smallFireball = new MyPetSmallFireball(world, entityMyPet, distanceX, distanceY, distanceZ);
            smallFireball.locY = (entityMyPet.locY + entityMyPet.length / 2.0F + 0.5D);
            smallFireball.setDamage(damage);
            world.addEntity(smallFireball);
            world.makeSound(entityMyPet.locX + 0.5D, entityMyPet.locY + 0.5D, entityMyPet.locZ + 0.5D, "mob.ghast.fireball", 1.0F + entityMyPet.getRandom().nextFloat(), entityMyPet.getRandom().nextFloat() * 0.7F + 0.3F);
        } else if (projectile == Projectiles.WitherSkull) {
            double distanceX = this.target.locX - entityMyPet.locX;
            double distanceY = this.target.boundingBox.b + this.target.length / 2.5F - (entityMyPet.locY + entityMyPet.length / 2.5F);
            double distanceZ = this.target.locZ - entityMyPet.locZ;
            MyPetWitherSkull witherSkull = new MyPetWitherSkull(world, entityMyPet, distanceX, distanceY, distanceZ);
            witherSkull.locY = (entityMyPet.locY + entityMyPet.length / 2.0F + 0.5D);
            witherSkull.setDamage(damage);
            world.addEntity(witherSkull);
            world.makeSound(entityMyPet.locX + 0.5D, entityMyPet.locY + 0.5D, entityMyPet.locZ + 0.5D, "mob.wither.shoot", 1.0F + entityMyPet.getRandom().nextFloat(), entityMyPet.getRandom().nextFloat() * 0.7F + 0.3F);
        }
    }
}
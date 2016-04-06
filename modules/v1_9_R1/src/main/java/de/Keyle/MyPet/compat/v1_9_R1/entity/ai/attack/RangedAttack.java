/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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

package de.Keyle.MyPet.compat.v1_9_R1.entity.ai.attack;

import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.ai.AIGoal;
import de.Keyle.MyPet.api.skill.Skills;
import de.Keyle.MyPet.api.skill.skills.RangedInfo;
import de.Keyle.MyPet.api.skill.skills.RangedInfo.Projectiles;
import de.Keyle.MyPet.compat.v1_9_R1.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_9_R1.skill.skills.ranged.nms.*;
import de.Keyle.MyPet.skill.skills.Ranged;
import net.minecraft.server.v1_9_R1.*;
import org.bukkit.craftbukkit.v1_9_R1.entity.CraftLivingEntity;

public class RangedAttack extends AIGoal {
    private MyPet myPet;
    private final EntityMyPet entityMyPet;
    private EntityLiving target;
    private int shootTimer;
    private float walkSpeedModifier;
    private int lastSeenTimer;
    private float range;
    private RangedInfo rangedSkill;

    public RangedAttack(EntityMyPet entityMyPet, float walkSpeedModifier, float range) {
        this.entityMyPet = entityMyPet;
        this.myPet = entityMyPet.getMyPet();
        this.shootTimer = -1;
        this.lastSeenTimer = 0;
        this.walkSpeedModifier = walkSpeedModifier;
        this.range = range * range;
        rangedSkill = entityMyPet.getMyPet().getSkills().getSkill(Ranged.class).get();
    }

    @Override
    public boolean shouldStart() {
        if (myPet.getRangedDamage() <= 0) {
            return false;
        }
        if (!entityMyPet.canMove()) {
            return false;
        }
        if (!entityMyPet.hasTarget()) {
            return false;
        }

        EntityLiving target = ((CraftLivingEntity) this.entityMyPet.getTarget()).getHandle();

        if (target instanceof EntityArmorStand) {
            return false;
        }
        double meleeDamage = myPet.getDamage();
        if (meleeDamage > 0 && this.entityMyPet.f(target.locX, target.getBoundingBox().b, target.locZ) < 4) {
            if (meleeDamage > rangedSkill.getDamage()) {
                return false;
            }
        }
        this.target = target;
        return true;
    }

    @Override
    public boolean shouldFinish() {
        if (!entityMyPet.hasTarget() || myPet.getRangedDamage() <= 0 || !entityMyPet.canMove()) {
            return true;
        }
        if (this.target.getBukkitEntity() != entityMyPet.getTarget()) {
            return true;
        }
        double meleeDamage = myPet.getDamage();
        if (meleeDamage > 0 && this.entityMyPet.f(target.locX, target.getBoundingBox().b, target.locZ) < 4) {
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

        this.entityMyPet.getPetNavigation().getParameters().removeSpeedModifier("RangedAttack");
    }

    @Override
    public void tick() {
        double distanceToTarget = this.entityMyPet.e(this.target.locX, this.target.getBoundingBox().b, this.target.locZ);
        boolean canSee = this.entityMyPet.getEntitySenses().a(this.target); // a -> canSee

        if (canSee) {
            this.lastSeenTimer++;
        } else {
            this.lastSeenTimer = 0;
        }

        if ((distanceToTarget <= this.range) && (this.lastSeenTimer >= 20)) {
            this.entityMyPet.getPetNavigation().getParameters().removeSpeedModifier("RangedAttack");
            this.entityMyPet.getPetNavigation().stop();
        } else {
            this.entityMyPet.getPetNavigation().getParameters().addSpeedModifier("RangedAttack", walkSpeedModifier);
            this.entityMyPet.getPetNavigation().navigateTo(this.target.getBukkitEntity().getLocation());
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
            return skills.getSkill(Ranged.class).get().getProjectile();
        }
        return Projectiles.Arrow;
    }

    public void shootProjectile(EntityLiving target, float damage, Projectiles projectile) {
        World world = target.world;

        if (projectile == Projectiles.Arrow) {
            EntityArrow arrow = new MyPetArrow(world, entityMyPet);
            arrow.c(damage);
            arrow.setCritical(false);
            entityMyPet.makeSound("entity.arrow.shoot", 1.0F, 1.0F / (entityMyPet.getRandom().nextFloat() * 0.4F + 0.8F));
            double distanceX = target.locX - entityMyPet.locX;
            double distanceY = target.locY + target.getHeadHeight() - 1.100000023841858D - arrow.locY;
            double distanceZ = target.locZ - entityMyPet.locZ;
            float distance20percent = MathHelper.sqrt(distanceX * distanceX + distanceZ * distanceZ) * 0.2F;
            arrow.shoot(distanceX, distanceY + distance20percent, distanceZ, 1.6F, 1);
            world.addEntity(arrow);
        } else if (projectile == Projectiles.Snowball) {
            MyPetSnowball snowball = new MyPetSnowball(world, entityMyPet);
            double distanceX = target.locX - entityMyPet.locX;
            double distanceY = target.locY + target.getHeadHeight() - 1.100000023841858D - snowball.locY;
            double distanceZ = target.locZ - entityMyPet.locZ;
            float distance20percent = MathHelper.sqrt(distanceX * distanceX + distanceZ * distanceZ) * 0.2F;
            snowball.setDamage(damage);
            snowball.shoot(distanceX, distanceY + distance20percent, distanceZ, 1.6F, 1);
            entityMyPet.makeSound("entity.arrow.shoot", 0.5F, 0.4F / (entityMyPet.getRandom().nextFloat() * 0.4F + 0.8F));
            world.addEntity(snowball);
        } else if (projectile == Projectiles.Egg) {
            MyPetEgg egg = new MyPetEgg(world, entityMyPet);
            double distanceX = target.locX - entityMyPet.locX;
            double distanceY = target.locY + target.getHeadHeight() - 1.100000023841858D - egg.locY;
            double distanceZ = target.locZ - entityMyPet.locZ;
            float distance20percent = MathHelper.sqrt(distanceX * distanceX + distanceZ * distanceZ) * 0.2F;
            egg.setDamage(damage);
            egg.shoot(distanceX, distanceY + distance20percent, distanceZ, 1.6F, 1);
            entityMyPet.makeSound("entity.arrow.shoot", 0.5F, 0.4F / (entityMyPet.getRandom().nextFloat() * 0.4F + 0.8F));
            world.addEntity(egg);
        } else if (projectile == Projectiles.LargeFireball) {
            double distanceX = this.target.locX - entityMyPet.locX;
            double distanceY = this.target.getBoundingBox().b + (double) (this.target.length / 2.0F) - (0.5D + entityMyPet.locY + (double) (entityMyPet.length / 2.0F));
            double distanceZ = this.target.locZ - entityMyPet.locZ;
            MyPetLargeFireball largeFireball = new MyPetLargeFireball(world, entityMyPet, distanceX, distanceY, distanceZ);
            largeFireball.locY = (entityMyPet.locY + entityMyPet.length / 2.0F + 0.5D);
            largeFireball.setDamage(damage);
            world.addEntity(largeFireball);
            entityMyPet.makeSound("entity.ghast.shoot", 1.0F + entityMyPet.getRandom().nextFloat(), entityMyPet.getRandom().nextFloat() * 0.7F + 0.3F);
        } else if (projectile == Projectiles.SmallFireball) {
            double distanceX = this.target.locX - entityMyPet.locX;
            double distanceY = this.target.getBoundingBox().b + (this.target.length / 2.0F) - (0.5D + entityMyPet.locY + (entityMyPet.length / 2.0F));
            double distanceZ = this.target.locZ - entityMyPet.locZ;
            MyPetSmallFireball smallFireball = new MyPetSmallFireball(world, entityMyPet, distanceX, distanceY, distanceZ);
            smallFireball.locY = (entityMyPet.locY + entityMyPet.length / 2.0F + 0.5D);
            smallFireball.setDamage(damage);
            world.addEntity(smallFireball);
            entityMyPet.makeSound("entity.ghast.shoot", 1.0F + entityMyPet.getRandom().nextFloat(), entityMyPet.getRandom().nextFloat() * 0.7F + 0.3F);
        } else if (projectile == Projectiles.WitherSkull) {
            double distanceX = this.target.locX - entityMyPet.locX;
            double distanceY = this.target.getBoundingBox().b + (double) (this.target.length / 2.0F) - (0.5D + entityMyPet.locY + (double) (entityMyPet.length / 2.0F));
            double distanceZ = this.target.locZ - entityMyPet.locZ;
            MyPetWitherSkull witherSkull = new MyPetWitherSkull(world, entityMyPet, distanceX, distanceY, distanceZ);
            witherSkull.locY = (entityMyPet.locY + entityMyPet.length / 2.0F + 0.5D);
            witherSkull.setDamage(damage);
            world.addEntity(witherSkull);
            entityMyPet.makeSound("entity.wither.shoot", 1.0F + entityMyPet.getRandom().nextFloat(), entityMyPet.getRandom().nextFloat() * 0.7F + 0.3F);
        }
    }
}
/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2020 Keyle
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

package de.Keyle.MyPet.compat.v1_21_R2.skill.skills.ranged.nms;

import de.Keyle.MyPet.api.entity.skill.ranged.EntityMyPetProjectile;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_21_R2.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_21_R2.skill.skills.ranged.bukkit.CraftMyPetLargeFireball;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

@Compat("v1_21_R2")
public class MyPetLargeFireball extends LargeFireball implements EntityMyPetProjectile {

    protected float damage = 0;
    protected int deathCounter = 100;
    protected CraftMyPetLargeFireball bukkitEntity = null;

    public MyPetLargeFireball(Level world, EntityMyPet entityliving, double d0, double d1, double d2) {
        super(world, entityliving, new Vec3(d0, d1, d2), 1);
    }

    @Override
    public EntityMyPet getShooter() {
        return (EntityMyPet) super.getOwner();
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    /*@Override
    public void assignDirectionalMovement(Vec3 vec3d, double d0) {
        double d1 = vec3d.x + this.random.nextGaussian() * 0.2D;
        double d2 = vec3d.y + this.random.nextGaussian() * 0.2D;
        double d3 = vec3d.z + this.random.nextGaussian() * 0.2D;

        this.setDeltaMovement(vec3d.normalize().scale(d0));
        this.hasImpulse = true;
    }*/

    @Override
    public CraftMyPetLargeFireball getBukkitEntity() {
        if (this.bukkitEntity == null) {
            this.bukkitEntity = new CraftMyPetLargeFireball(this.level().getCraftServer(), this);
        }
        return this.bukkitEntity;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbtTagCompound) {
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbtTagCompound) {
    }

    @Override
    protected void onHit(HitResult movingObjectPosition) {
        if (movingObjectPosition.getType() == HitResult.Type.ENTITY) {
            Entity entity = ((EntityHitResult) movingObjectPosition).getEntity();
            if (entity instanceof LivingEntity) {
                entity.hurtServer(this.level().getMinecraftWorld(), this.damageSources().fireball(this, getShooter()), damage);
            }
        }
        discard();
    }

    @Override
    public void tick() {
        try {
            super.tick();
            if (deathCounter-- <= 0) {
                discard();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damagesource, float f) {
        return false;
    }
}

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

package de.Keyle.MyPet.compat.v1_19_R1_2.skill.skills.ranged.nms;

import de.Keyle.MyPet.api.entity.skill.ranged.EntityMyPetProjectile;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_19_R1_2.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_19_R1_2.skill.skills.ranged.bukkit.CraftMyPetDragonFireball;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

@Compat("v1_19_R1_2")
public class MyPetDragonFireball extends DragonFireball implements EntityMyPetProjectile {

    protected float damage = 0;
    protected int deathCounter = 100;
    protected CraftMyPetDragonFireball bukkitEntity = null;

    public MyPetDragonFireball(Level world, EntityMyPet entityliving, double d0, double d1, double d2) {
        super(world, entityliving, d0, d1, d2);
    }

    @Override
    public EntityMyPet getShooter() {
        return (EntityMyPet) super.getOwner();
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    @Override
    public void setDirection(double d0, double d1, double d2) {
        d0 += this.random.nextGaussian() * 0.2D;
        d1 += this.random.nextGaussian() * 0.2D;
        d2 += this.random.nextGaussian() * 0.2D;
        double d3 = Mth.sqrt((float) (d0 * d0 + d1 * d1 + d2 * d2));
        this.xPower = (d0 / d3 * 0.1D);
        this.yPower = (d1 / d3 * 0.1D);
        this.zPower = (d2 / d3 * 0.1D);
    }

    @Override
    public CraftMyPetDragonFireball getBukkitEntity() {
        if (this.bukkitEntity == null) {
            this.bukkitEntity = new CraftMyPetDragonFireball(this.level.getCraftServer(), this);
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
                entity.hurt(DamageSource.thrown(this, getShooter()), damage);
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
}

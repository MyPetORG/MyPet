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
import de.Keyle.MyPet.api.util.ReflectionUtil;
import de.Keyle.MyPet.compat.v1_21_R2.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_21_R2.skill.skills.ranged.bukkit.CraftMyPetEnderPearl;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.lang.reflect.Field;

@Compat("v1_21_R2")
public class MyPetEnderPearl extends ThrownEnderpearl implements EntityMyPetProjectile {

    protected float damage = 0;
    protected CraftMyPetEnderPearl bukkitEntity = null;

    public MyPetEnderPearl(Level world, EntityMyPet entityLiving) {
        super(world, entityLiving, new ItemStack(Items.ENDER_PEARL));
    }

    @Override
    public EntityMyPet getShooter() {
        return (EntityMyPet) super.getOwner();
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    @Override
    public CraftMyPetEnderPearl getBukkitEntity() {
        if (this.bukkitEntity == null) {
            this.bukkitEntity = new CraftMyPetEnderPearl(this.level().getCraftServer(), this);
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
                entity.hurtServer(this.level().getMinecraftWorld(), this.damageSources().thrown(this, getShooter()), damage);
            }
        }
        for (int i = 0; i < 32; ++i) {
            // ParticleTypes.PORTAL
            // This was actually Mapped correctly but for *whatever reason* it... didn't work?
            Field portalParticleField = ReflectionUtil.getField(ParticleTypes.class,"ad");
            ParticleOptions portalParticle = (ParticleOptions) ReflectionUtil.getFieldValue(portalParticleField, null);
            this.level().addParticle(portalParticle, this.getX(), this.getY() + this.random.nextDouble() * 2.0D, this.getZ(), this.random.nextGaussian(), 0.0D, this.random.nextGaussian());
        }
        discard();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damagesource, float f) {
        return false;
    }
}

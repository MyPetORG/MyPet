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

package de.Keyle.MyPet.compat.v1_17_R1.skill.skills.ranged.nms;

import de.Keyle.MyPet.api.entity.skill.ranged.EntityMyPetProjectile;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_17_R1.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_17_R1.skill.skills.ranged.bukkit.CraftMyPetEgg;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

@Compat("v1_17_R1")
public class MyPetEgg extends ThrownEgg implements EntityMyPetProjectile {

    protected float damage = 0;
    protected CraftMyPetEgg bukkitEntity = null;

    public MyPetEgg(Level world, EntityMyPet entityLiving) {
        super(world, entityLiving);
    }

    @Override
    public EntityMyPet getShooter() {
        return (EntityMyPet) super.getOwner();
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    @Override
    public CraftMyPetEgg getBukkitEntity() {
        if (this.bukkitEntity == null) {
            this.bukkitEntity = new CraftMyPetEgg(this.level.getCraftServer(), this);
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
        for (int i = 0; i < 8; ++i) {
            this.level.addParticle(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.LEATHER_BOOTS)), this.getX(), this.getY(), this.getZ(), ((double) this.random.nextFloat() - 0.5D) * 0.08D, ((double) this.random.nextFloat() - 0.5D) * 0.08D, ((double) this.random.nextFloat() - 0.5D) * 0.08D);
        }
        discard();
    }

    @Override
    public boolean hurt(DamageSource damagesource, float f) {
        return false;
    }
}

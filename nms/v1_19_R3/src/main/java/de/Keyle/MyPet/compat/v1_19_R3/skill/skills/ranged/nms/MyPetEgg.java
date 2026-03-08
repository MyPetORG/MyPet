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

package de.Keyle.MyPet.compat.v1_19_R3.skill.skills.ranged.nms;

import de.Keyle.MyPet.api.entity.skill.ranged.EntityMyPetProjectile;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_19_R3.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_19_R3.skill.skills.ranged.bukkit.CraftMyPetEgg;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

@Compat("v1_19_R3")
public class MyPetEgg extends ThrownEgg implements EntityMyPetProjectile {

    protected float damage = 0;
    protected CraftMyPetEgg bukkitEntity = null;

    public MyPetEgg(Level world, EntityMyPet entityLiving) {
        super(world, entityLiving);
    }

    @Override
    @Nullable
    public org.bukkit.entity.Entity getShooter() {
        Entity owner = super.getOwner();
        return owner != null ? owner.getBukkitEntity() : null;
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
                entity.hurt(this.damageSources().thrown(this, super.getOwner()), damage);
            }
        }
        getBukkitEntity().getWorld().spawnParticle(org.bukkit.Particle.ITEM_CRACK, getX(), getY(), getZ(), 8, 0.0, 0.0, 0.0, 0.04, new org.bukkit.inventory.ItemStack(org.bukkit.Material.EGG));
        discard();
    }

    @Override
    public boolean hurt(DamageSource damagesource, float f) {
        return false;
    }
}

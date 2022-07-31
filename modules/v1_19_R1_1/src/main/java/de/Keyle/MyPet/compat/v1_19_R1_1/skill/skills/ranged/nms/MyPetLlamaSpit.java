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

package de.Keyle.MyPet.compat.v1_19_R1_1.skill.skills.ranged.nms;

import de.Keyle.MyPet.api.entity.skill.ranged.EntityMyPetProjectile;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_19_R1_1.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_19_R1_1.skill.skills.ranged.bukkit.CraftMyPetLlamaSpit;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

@Compat("v1_19_R1_1")
public class MyPetLlamaSpit extends LlamaSpit implements EntityMyPetProjectile {

    @Setter
    @Getter
    protected float damage = 0;
    protected CraftMyPetLlamaSpit bukkitEntity = null;

    public MyPetLlamaSpit(Level world, EntityMyPet entityMyPet) {
        super(EntityType.LLAMA_SPIT, world);
        this.setOwner(entityMyPet);
        this.setPos(entityMyPet.getX() - (double) (entityMyPet.getBbWidth() + 1.0F) * 0.5D * (double) Mth.sin(entityMyPet.EXTRA_RENDER_CULLING_SIZE_WITH_BIG_HAT * 0.017453292F),
                entityMyPet.getY() + (double) entityMyPet.getEyeHeight() - 0.10000000149011612D,
                entityMyPet.getZ() + (double) (entityMyPet.getBbWidth() + 1.0F) * 0.5D * (double) Mth.cos(entityMyPet.EXTRA_RENDER_CULLING_SIZE_WITH_BIG_HAT * 0.017453292F));
    }

    @Override
    public EntityMyPet getShooter() {
        return (EntityMyPet) super.getOwner();
    }

    @Override
    public CraftMyPetLlamaSpit getBukkitEntity() {
        if (this.bukkitEntity == null) {
            this.bukkitEntity = new CraftMyPetLlamaSpit(this.level.getCraftServer(), this);
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
    public boolean hurt(DamageSource damagesource, float f) {
        return false;
    }

    @Override
    public void onHit(HitResult movingObjectPosition) {
        if (movingObjectPosition.getType() == HitResult.Type.ENTITY) {
            Entity entity = ((EntityHitResult) movingObjectPosition).getEntity();
            if (entity instanceof LivingEntity) {
                entity.hurt(DamageSource.indirectMobAttack(this, getShooter()), damage);
            }
        }
        this.discard();
    }
}

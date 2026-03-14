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

package de.Keyle.MyPet.compat.v1_21_R3.skill.skills.ranged.nms;

import de.Keyle.MyPet.api.entity.skill.ranged.EntityMyPetProjectile;
import de.Keyle.MyPet.api.util.ErrorUtil;
import de.Keyle.MyPet.compat.v1_21_R3.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_21_R3.skill.skills.ranged.bukkit.CraftMyPetTrident;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class MyPetTrident extends ThrownTrident implements EntityMyPetProjectile {

    protected CraftMyPetTrident bukkitEntity = null;

    public MyPetTrident(Level world, EntityMyPet entityMyPet) {
        super(world, entityMyPet, new ItemStack(Items.TRIDENT));
    }

    @Override
    @Nullable
    public org.bukkit.entity.Entity getShooter() {
        Entity owner = super.getOwner();
        return owner != null ? owner.getBukkitEntity() : null;
    }

    @Override
    public CraftMyPetTrident getBukkitEntity() {
        if (this.bukkitEntity == null) {
            this.bukkitEntity = new CraftMyPetTrident(this.level().getCraftServer(), this);
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
    public void tick() {
        try {
            super.tick();
            if (this.isInGround()) {    //TODO
                discard();
            }
        } catch (Exception e) {
            ErrorUtil.report(e);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damagesource, float f) {
        return false;
    }
}

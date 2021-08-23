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
import de.Keyle.MyPet.compat.v1_17_R1.skill.skills.ranged.bukkit.CraftMyPetTrident;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

@Compat("v1_17_R1")
public class MyPetTrident extends ThrownTrident implements EntityMyPetProjectile {

    protected CraftMyPetTrident bukkitEntity = null;

    public MyPetTrident(Level world, EntityMyPet entityMyPet) {
        super(world, entityMyPet, new ItemStack(Items.TRIDENT));
    }

    @Override
    public EntityMyPet getShooter() {
        return (EntityMyPet) super.getOwner();
    }

    @Override
    public CraftMyPetTrident getBukkitEntity() {
        if (this.bukkitEntity == null) {
            this.bukkitEntity = new CraftMyPetTrident(this.level.getCraftServer(), this);
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
            if (this.b) {	//TODO
                discard();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hurt(DamageSource damagesource, float f) {
        return false;
    }
}

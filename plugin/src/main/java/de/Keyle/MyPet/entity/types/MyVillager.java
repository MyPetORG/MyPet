/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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

package de.Keyle.MyPet.entity.types;

import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.MyPet;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

@Getter
public class MyVillager extends MyPet implements de.Keyle.MyPet.api.entity.types.MyVillager {

    protected int profession = 0;
    protected Type type = Type.Plains;
    protected int level = 1;
    @Setter
    protected CompoundBinaryTag originalData = null;

    public MyVillager(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public CompoundBinaryTag writeExtendedInfo() {
        CompoundBinaryTag info = super.writeExtendedInfo();
        info = info.putInt("Profession", getProfession())
                   .putInt("VillagerType", getType().ordinal())
                   .putInt("VillagerLevel", this.getLevel());
        if (originalData != null) {
            info = info.put("OriginalData", originalData);
        }
        return info;
    }

    @Override
    public void readExtendedInfo(CompoundBinaryTag info) {
        super.readExtendedInfo(info);
        if (info.keySet().contains("Profession")) {
            setProfession(info.getInt("Profession"));
        }
        if (info.keySet().contains("VillagerType")) {
            setType(Type.values()[info.getInt("VillagerType")]);
        }
        if (info.keySet().contains("VillagerLevel")) {
            setLevel(info.getInt("VillagerLevel"));
        }
        if (info.keySet().contains("OriginalData")) {
            originalData = info.getCompound("OriginalData");
        }
    }

    public void setProfession(int value) {
        this.profession = value;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    @Override
    public void setType(Type value) {
        this.type = value;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    @Override
    public void setLevel(int level) {
        this.level = Math.max(1, level);
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    public boolean hasOriginalData() {
        return this.originalData != null;
    }

    @Override
    public void setEquipment(EquipmentSlot slot, ItemStack item) {
        if (slot != EquipmentSlot.HAND) {
            return;
        }
        super.setEquipment(slot, item);
    }
}
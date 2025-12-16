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
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagInt;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

@Getter
public class MyVillager extends MyPet implements de.Keyle.MyPet.api.entity.types.MyVillager {

    protected int profession = 0;
    protected Type type = Type.Plains;
    protected int level = 1;
    @Setter
    protected TagCompound originalData = null;

    public MyVillager(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public TagCompound writeExtendedInfo() {
        TagCompound info = super.writeExtendedInfo();
        info.getCompoundData().put("Profession", new TagInt(getProfession()));
        info.getCompoundData().put("VillagerType", new TagInt(getType().ordinal()));
        info.getCompoundData().put("VillagerLevel", new TagInt(this.getLevel()));
        if (originalData != null) {
            info.getCompoundData().put("OriginalData", originalData);
        }
        return info;
    }

    @Override
    public void readExtendedInfo(TagCompound info) {
        super.readExtendedInfo(info);
        if (info.containsKey("Profession")) {
            setProfession(info.getAs("Profession", TagInt.class).getIntData());
        }
        if (info.containsKey("VillagerType")) {
            setType(Type.values()[info.getAs("VillagerType", TagInt.class).getIntData()]);
        }
        if (info.containsKey("VillagerLevel")) {
            setLevel(info.getAs("VillagerLevel", TagInt.class).getIntData());
        }
        if (info.containsKey("OriginalData")) {
            originalData = info.getAs("OriginalData", TagCompound.class);
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
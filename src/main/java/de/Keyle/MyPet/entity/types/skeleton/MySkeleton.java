/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

package de.Keyle.MyPet.entity.types.skeleton;

import de.Keyle.MyPet.entity.EquipmentSlot;
import de.Keyle.MyPet.entity.MyPetInfo;
import de.Keyle.MyPet.entity.types.IMyPetEquipment;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.skill.skills.implementation.inventory.ItemStackNBTConverter;
import de.Keyle.MyPet.util.player.MyPetPlayer;
import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagInt;
import de.keyle.knbt.TagList;
import net.minecraft.server.v1_8_R1.ItemStack;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.bukkit.Material.BONE;

@MyPetInfo(food = {BONE})
public class MySkeleton extends MyPet implements IMyPetEquipment {
    protected boolean isWither = false;
    protected Map<EquipmentSlot, ItemStack> equipment = new HashMap<EquipmentSlot, ItemStack>();

    public MySkeleton(MyPetPlayer petOwner) {
        super(petOwner);
    }

    public ItemStack[] getEquipment() {
        ItemStack[] equipment = new ItemStack[EquipmentSlot.values().length];
        for (int i = 0; i < EquipmentSlot.values().length; i++) {
            equipment[i] = getEquipment(EquipmentSlot.getSlotById(i));
        }
        return equipment;
    }

    public ItemStack getEquipment(EquipmentSlot slot) {
        return equipment.get(slot);
    }

    @Override
    public TagCompound getExtendedInfo() {
        TagCompound info = super.getExtendedInfo();
        info.getCompoundData().put("Wither", new TagByte(isWither()));

        List<TagCompound> itemList = new ArrayList<TagCompound>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (getEquipment(slot) != null) {
                TagCompound item = ItemStackNBTConverter.itemStackToCompund(getEquipment(slot));
                item.getCompoundData().put("Slot", new TagInt(slot.getSlotId()));
                itemList.add(item);
            }
        }
        info.getCompoundData().put("Equipment", new TagList(itemList));
        return info;
    }

    @Override
    public void setExtendedInfo(TagCompound info) {
        if (info.getCompoundData().containsKey("Wither")) {
            setWither(info.getAs("Wither", TagByte.class).getBooleanData());
        }
        if (info.getCompoundData().containsKey("Equipment")) {
            TagList equipment = info.getAs("Equipment", TagList.class);
            for (int i = 0; i < equipment.size(); i++) {
                TagCompound item = equipment.getTagAs(i, TagCompound.class);

                ItemStack itemStack = ItemStackNBTConverter.compundToItemStack(item);
                setEquipment(EquipmentSlot.getSlotById(item.getAs("Slot", TagInt.class).getIntData()), itemStack);
            }
        }
    }

    @Override
    public MyPetType getPetType() {
        return MyPetType.Skeleton;
    }

    public boolean isWither() {
        return isWither;
    }

    public void setWither(boolean flag) {
        if (status == PetState.Here) {
            ((EntityMySkeleton) getCraftPet().getHandle()).setWither(flag);
        }
        this.isWither = flag;
    }

    public void setEquipment(EquipmentSlot slot, ItemStack item) {
        if (item == null) {
            equipment.remove(slot);
            ((EntityMySkeleton) getCraftPet().getHandle()).setPetEquipment(slot.getSlotId(), null);
            return;
        }
        item = item.cloneItemStack();
        item.count = 1;
        equipment.put(slot, item);
        if (status == PetState.Here) {
            ((EntityMySkeleton) getCraftPet().getHandle()).setPetEquipment(slot.getSlotId(), item);
        }
    }

    @Override
    public String toString() {
        return "MySkeleton{owner=" + getOwner().getName() + ", name=" + ChatColor.stripColor(petName) + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skillTree != null ? skillTree.getName() : "-") + ", worldgroup=" + worldGroup + "}";
    }
}
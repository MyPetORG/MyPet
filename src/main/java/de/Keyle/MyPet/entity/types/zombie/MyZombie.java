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

package de.Keyle.MyPet.entity.types.zombie;

import de.Keyle.MyPet.entity.EquipmentSlot;
import de.Keyle.MyPet.entity.MyPetInfo;
import de.Keyle.MyPet.entity.types.IMyPetBaby;
import de.Keyle.MyPet.entity.types.IMyPetEquipment;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.skill.skills.implementation.inventory.ItemStackNBTConverter;
import de.Keyle.MyPet.util.ConfigItem;
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

import static org.bukkit.Material.ROTTEN_FLESH;

@MyPetInfo(food = {ROTTEN_FLESH})
public class MyZombie extends MyPet implements IMyPetEquipment, IMyPetBaby {
    public static ConfigItem GROW_UP_ITEM;

    protected boolean isBaby = false;
    protected boolean isVillager = false;
    protected Map<EquipmentSlot, ItemStack> equipment = new HashMap<EquipmentSlot, ItemStack>();

    public MyZombie(MyPetPlayer petOwner) {
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
    public TagCompound writeExtendedInfo() {
        TagCompound info = super.writeExtendedInfo();
        info.getCompoundData().put("Baby", new TagByte(isBaby()));
        info.getCompoundData().put("Villager", new TagByte(isVillager()));

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
    public void readExtendedInfo(TagCompound info) {
        if (info.getCompoundData().containsKey("Baby")) {
            setBaby(info.getAs("Baby", TagByte.class).getBooleanData());
        }
        if (info.getCompoundData().containsKey("Villager")) {
            setVillager(info.getAs("Villager", TagByte.class).getBooleanData());
        }
        if (info.getCompoundData().containsKey("Equipment")) {
            TagList equipment = info.get("Equipment");
            for (int i = 0; i < equipment.size(); i++) {
                TagCompound item = equipment.getTag(i);

                ItemStack itemStack = ItemStackNBTConverter.compundToItemStack(item);
                setEquipment(EquipmentSlot.getSlotById(item.getAs("Slot", TagInt.class).getIntData()), itemStack);
            }
        }
    }

    @Override
    public MyPetType getPetType() {
        return MyPetType.Zombie;
    }

    public boolean isBaby() {
        return isBaby;
    }

    public void setBaby(boolean flag) {
        if (status == PetState.Here) {
            ((EntityMyZombie) getCraftPet().getHandle()).setBaby(flag);
        }
        this.isBaby = flag;
    }

    public boolean isVillager() {
        return isVillager;
    }

    public void setVillager(boolean flag) {
        if (status == PetState.Here) {
            ((EntityMyZombie) getCraftPet().getHandle()).setVillager(flag);
        }
        this.isVillager = flag;
    }

    public void setEquipment(EquipmentSlot slot, ItemStack item) {
        if (item == null) {
            equipment.remove(slot);
            ((EntityMyZombie) getCraftPet().getHandle()).setPetEquipment(slot.getSlotId(), null);
            return;
        }
        item = item.cloneItemStack();
        item.count = 1;
        equipment.put(slot, item);
        if (status == PetState.Here) {
            ((EntityMyZombie) getCraftPet().getHandle()).setPetEquipment(slot.getSlotId(), item);
        }
    }

    @Override
    public String toString() {
        return "MyZombie{owner=" + getOwner().getName() + ", name=" + ChatColor.stripColor(petName) + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skillTree != null ? skillTree.getName() : "-") + ", worldgroup=" + worldGroup + ", villager=" + isVillager() + ", baby=" + isBaby() + "}";
    }
}
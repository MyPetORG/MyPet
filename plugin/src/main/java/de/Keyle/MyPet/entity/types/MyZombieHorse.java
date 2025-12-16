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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.MyPet;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagList;
import de.keyle.knbt.TagString;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

@Getter
public class MyZombieHorse extends MyPet implements de.Keyle.MyPet.api.entity.types.MyZombieHorse {

    protected ItemStack saddle = null;

    public MyZombieHorse(MyPetPlayer petOwner) {
        super(petOwner);
    }

    public void setSaddle(ItemStack item) {
        if (item != null && item.getType() != Material.SADDLE) {
            return;
        }
        this.saddle = item;
        if (this.saddle != null) {
            this.saddle.setAmount(1);
        }
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    public boolean hasSaddle() {
        return saddle != null;
    }

    @Override
    public TagCompound writeExtendedInfo() {
        TagCompound info = super.writeExtendedInfo();

        // Write saddle with string slot name for MC versions before 1.21 (which lack EquipmentSlot.SADDLE)
        if (hasSaddle() && MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.21") < 0) {
            List<TagCompound> itemList = new ArrayList<>();
            if (info.containsKey("Equipment")) {
                itemList.addAll((List<TagCompound>) info.getAs("Equipment", TagList.class).getData());
            }
            TagCompound item = MyPetApi.getPlatformHelper().itemStackToCompund(getSaddle());
            item.getCompoundData().put("Slot", new TagString("SADDLE"));
            itemList.add(item);
            info.put("Equipment", new TagList(itemList));
        }
        return info;
    }

    // MyPetEquipment implementation

    @Override
    public ItemStack[] getEquipment() {
        return new ItemStack[]{saddle};
    }

    @Override
    public ItemStack getEquipment(EquipmentSlot slot) {
        if (slot.name().equals("SADDLE")) return saddle;
        return null;
    }

    @Override
    public void setEquipment(EquipmentSlot slot, ItemStack item) {
        setEquipmentBySlotName(slot.name(), item);
    }

    @Override
    protected void setEquipmentBySlotName(String slotName, ItemStack item) {
        if (slotName.equals("SADDLE")) {
            setSaddle(item);
        } else {
            super.setEquipmentBySlotName(slotName, item);
        }
    }

    @Override
    public void dropEquipment() {
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> {
                if (hasSaddle()) {
                    entity.getWorld().dropItem(entity.getLocation(), getSaddle());
                }
            });
            setSaddle(null);
        }
    }
}

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
import de.Keyle.MyPet.api.entity.EquipmentSlot;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.MyPet;
import de.keyle.knbt.TagCompound;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class MyAllay extends MyPet implements de.Keyle.MyPet.api.entity.types.MyAllay {

    protected ItemStack leItem;

    protected Map<EquipmentSlot, ItemStack> equipment = new HashMap<>();
    protected boolean isGlowing = false;

    public MyAllay(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public MyPetType getPetType() {
        return MyPetType.Allay;
    }

    @Override
    public String toString() {
        return "MyAllay{owner=" + getOwner().getName() + ", name=" + ChatColor.stripColor(petName) + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skilltree != null ? skilltree.getName() : "-") + ", worldgroup=" + worldGroup + "}";
    }

    @Override
    public ItemStack[] getEquipment() {
        ItemStack[] equipment = new ItemStack[EquipmentSlot.values().length];
        for (int i = 0; i < EquipmentSlot.values().length; i++) {
            equipment[i] = getEquipment(EquipmentSlot.getSlotById(i));
        }
        return equipment;
    }

    @Override
    public ItemStack getEquipment(EquipmentSlot slot) {
        return slot == EquipmentSlot.MainHand ? leItem : null;
    }

    @Override
    public void setEquipment(EquipmentSlot slot, ItemStack item) {
        if (slot == EquipmentSlot.MainHand) {
            if (item == null) {
                leItem = null;
                getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
                return;
            }

            item = item.clone();
            item.setAmount(1);
            leItem = item;
            if (status == PetState.Here) {
                getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
            }
        }
    }

    @Override
    public void dropEquipment() {
        if (getStatus() == PetState.Here) {
            if (leItem != null && leItem.getType() != Material.AIR) {
                Location dropLocation = getLocation().get();
                dropLocation.getWorld().dropItem(dropLocation, leItem);
            }
        }
    }

    @Override
    public TagCompound writeExtendedInfo() {
        TagCompound info = super.writeExtendedInfo();
        if (getEquipment(EquipmentSlot.MainHand) != null && getEquipment(EquipmentSlot.MainHand).getType() != Material.AIR) {
            TagCompound item = MyPetApi.getPlatformHelper().itemStackToCompund(getEquipment(EquipmentSlot.MainHand));
            info.getCompoundData().put("LeItem", item);
        }
        return info;
    }

    @Override
    public void readExtendedInfo(TagCompound info) {
        if (info.containsKey("LeItem")) {
            TagCompound item = info.getAs("LeItem", TagCompound.class);
            try {
                ItemStack itemStack = MyPetApi.getPlatformHelper().compundToItemStack(item);
                setEquipment(EquipmentSlot.MainHand, itemStack);
            } catch (Exception e) {
                MyPetApi.getLogger().warning("Could not load Equipment item from pet data!");
            }
        }
    }
}
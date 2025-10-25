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

public class MyPillager extends MyPet implements de.Keyle.MyPet.api.entity.types.MyPillager {

    protected ItemStack weapon;
    protected ItemStack banner;

    public MyPillager(MyPetPlayer petOwner) {
        super(petOwner);
    }

    @Override
    public MyPetType getPetType() {
        return MyPetType.Pillager;
    }

    @Override
    public String toString() {
        return "MyPillager{owner=" + getOwner().getName() + ", name=" + ChatColor.stripColor(petName) + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skilltree != null ? skilltree.getName() : "-") + ", worldgroup=" + worldGroup + "}";
    }

    @Override
    public TagCompound writeExtendedInfo() {
        TagCompound info = super.writeExtendedInfo();
        if (getEquipment(EquipmentSlot.MainHand) != null && getEquipment(EquipmentSlot.MainHand).getType() != Material.AIR) {
            TagCompound item = MyPetApi.getPlatformHelper().itemStackToCompund(getEquipment(EquipmentSlot.MainHand));
            info.getCompoundData().put("Weapon", item);
        }
        if (getEquipment(EquipmentSlot.Helmet) != null && getEquipment(EquipmentSlot.Helmet).getType() != Material.AIR) {
            TagCompound item = MyPetApi.getPlatformHelper().itemStackToCompund(getEquipment(EquipmentSlot.Helmet));
            info.getCompoundData().put("Banner", item);
        }
        return info;
    }

    @Override
    public void readExtendedInfo(TagCompound info) {
        if (info.containsKey("Weapon")) {
            TagCompound item = info.getAs("Weapon", TagCompound.class);
            try {
                ItemStack itemStack = MyPetApi.getPlatformHelper().compundToItemStack(item);
                setEquipment(EquipmentSlot.MainHand, itemStack);
            } catch (Exception e) {
                MyPetApi.getLogger().warning("Could not load Equipment item from pet data!");
            }
        }
        if (info.containsKey("Banner")) {
            TagCompound item = info.getAs("Banner", TagCompound.class);
            try {
                ItemStack itemStack = MyPetApi.getPlatformHelper().compundToItemStack(item);
                setEquipment(EquipmentSlot.Helmet, itemStack);
            } catch (Exception e) {
                MyPetApi.getLogger().warning("Could not load Equipment item from pet data!");
            }
        }
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
        if (slot == EquipmentSlot.MainHand) {
            return weapon;
        } else if (slot == EquipmentSlot.Helmet) {
            return banner;
        } else {
            return null;
        }
    }

    @Override
    public void setEquipment(EquipmentSlot slot, ItemStack item) {
        if (slot == EquipmentSlot.MainHand || slot == EquipmentSlot.Helmet) {
            if (item == null) {
                if (slot == EquipmentSlot.MainHand) {
                    weapon = null;
                } else {
                    banner = null;
                }
                getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
                return;
            }

            item = item.clone();
            item.setAmount(1);
            if (slot == EquipmentSlot.MainHand) {
                weapon = item;
            } else {
                banner = item;
            }
            if (status == PetState.Here) {
                getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
            }
        }
    }

    @Override
    public void dropEquipment() {
        if (getStatus() == PetState.Here) {
            if (weapon != null && weapon.getType() != Material.AIR) {
                Location dropLocation = getLocation().get();
                dropLocation.getWorld().dropItem(dropLocation, weapon);
            }
            if (banner != null && banner.getType() != Material.AIR) {
                Location dropLocation = getLocation().get();
                dropLocation.getWorld().dropItem(dropLocation, banner);
            }
        }
    }
}
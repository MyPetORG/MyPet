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
import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagInt;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Fox.Type;
import org.bukkit.inventory.ItemStack;

public class MyFox extends MyPet implements de.Keyle.MyPet.api.entity.types.MyFox {

    protected ItemStack weapon;
    protected boolean isBaby = false;
    protected Type foxType = Type.RED;

    public MyFox(MyPetPlayer petOwner) {
        super(petOwner);
    }

    public Type getType() {
        return foxType;
    }

    public void setType(Type value) {
        this.foxType = value;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    public boolean isBaby() {
        return isBaby;
    }

    public void setBaby(boolean flag) {
        this.isBaby = flag;
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
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
        return slot == EquipmentSlot.MainHand ? weapon : null;
    }

    @Override
    public void setEquipment(EquipmentSlot slot, ItemStack item) {
        if (slot == EquipmentSlot.MainHand) {
            if (item == null) {
                weapon = null;
                getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
                return;
            }

            item = item.clone();
            item.setAmount(1);
            weapon = item;
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
        }
    }

    @Override
    public TagCompound writeExtendedInfo() {
        TagCompound info = super.writeExtendedInfo();
        info.getCompoundData().put("FoxType", new TagInt(getType().ordinal()));
        info.getCompoundData().put("Baby", new TagByte(isBaby()));
        if (getEquipment(EquipmentSlot.MainHand) != null && getEquipment(EquipmentSlot.MainHand).getType() != Material.AIR) {
            TagCompound item = MyPetApi.getPlatformHelper().itemStackToCompund(getEquipment(EquipmentSlot.MainHand));
            info.getCompoundData().put("MouthItem", item);
        }
        return info;
    }

    @Override
    public void readExtendedInfo(TagCompound info) {
        if (info.containsKey("FoxType")) {
            setType(Type.values()[info.getAs("FoxType", TagInt.class).getIntData()]);
        }
        if (info.containsKey("Baby")) {
            setBaby(info.getAs("Baby", TagByte.class).getBooleanData());
        }
        if (info.containsKey("MouthItem")) {
            TagCompound item = info.getAs("MouthItem", TagCompound.class);
            try {
                ItemStack itemStack = MyPetApi.getPlatformHelper().compundToItemStack(item);
                setEquipment(EquipmentSlot.MainHand, itemStack);
            } catch (Exception e) {
                MyPetApi.getLogger().warning("Could not load Equipment item from pet data!");
            }
        }
    }

    @Override
    public MyPetType getPetType() {
        return MyPetType.Fox;
    }

    @Override
    public String toString() {
        return "MyFox{owner=" + getOwner().getName() + ", name=" + ChatColor.stripColor(petName) + ", exp=" + experience.getExp() + "/" + experience.getRequiredExp() + ", lv=" + experience.getLevel() + ", status=" + status.name() + ", skilltree=" + (skilltree != null ? skilltree.getName() : "-") + ", worldgroup=" + worldGroup + ", foxtype=" + getType().name() + ", baby=" + isBaby() + "}";
    }
}
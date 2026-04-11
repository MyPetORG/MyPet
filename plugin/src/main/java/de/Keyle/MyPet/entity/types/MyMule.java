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
import lombok.Getter;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

@Getter
public class MyMule extends MyPet implements de.Keyle.MyPet.api.entity.types.MyMule {

    protected ItemStack chest = null;
    protected ItemStack saddle = null;

    public MyMule(MyPetPlayer petOwner) {
        super(petOwner);
    }

    public void setChest(ItemStack item) {
        if (item != null && item.getType() != Material.CHEST && item.getType() != Material.TRAPPED_CHEST) {
            return;
        }
        this.chest = item;
        if (this.chest != null) {
            this.chest.setAmount(1);
        }
        if (status == PetState.Here) {
            updateVisuals();
        }
    }

    public boolean hasChest() {
        return chest != null;
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
            updateVisuals();
        }
    }

    public boolean hasSaddle() {
        return saddle != null;
    }

    @Override
    public CompoundBinaryTag writeExtendedInfo() {
        CompoundBinaryTag info = super.writeExtendedInfo();
        CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();
        for (String key : info.keySet()) {
            builder.put(key, info.get(key));
        }
        // Chest is not an equipment slot, write separately
        if (hasChest()) {
            builder.put("Chest", MyPetApi.getPlatformHelper().itemStackToCompound(getChest()));
        }
        // Write saddle with string slot name for MC versions before 1.21 (which lack EquipmentSlot.SADDLE)
        if (hasSaddle() && !MyPetApi.getCompatUtil().minecraftVersionEqualsOrAbove("1.21")) {
            List<BinaryTag> itemList = new ArrayList<>();
            if (info.keySet().contains("Equipment")) {
                ListBinaryTag existingEquip = info.getList("Equipment");
                for (int i = 0; i < existingEquip.size(); i++) {
                    itemList.add(existingEquip.getCompound(i));
                }
            }
            CompoundBinaryTag item = MyPetApi.getPlatformHelper().itemStackToCompound(getSaddle());
            item = item.putString("Slot", "SADDLE");
            itemList.add(item);
            builder.put("Equipment", ListBinaryTag.listBinaryTag(BinaryTagTypes.COMPOUND, itemList));
        }
        return builder.build();
    }

    @Override
    public void readExtendedInfo(CompoundBinaryTag info) {
        super.readExtendedInfo(info);
        // Chest is not an equipment slot, read separately
        BinaryTag chestTag = info.get("Chest");
        if (chestTag != null) {
            if (chestTag.type() == BinaryTagTypes.BYTE) {
                boolean chest = info.getBoolean("Chest");
                if (chest) {
                    ItemStack item = new ItemStack(Material.CHEST);
                    setChest(item);
                }
            } else if (chestTag.type() == BinaryTagTypes.COMPOUND) {
                CompoundBinaryTag itemTag = info.getCompound("Chest");
                try {
                    ItemStack item = MyPetApi.getPlatformHelper().compoundToItemStack(itemTag);
                    setChest(item);
                } catch (Exception e) {
                    MyPetApi.getLogger().warning("Could not load Chest item from pet data!");
                }
            }
        }
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
                if (hasChest()) {
                    entity.getWorld().dropItem(entity.getLocation(), getChest());
                }
            });
            setSaddle(null);
            setChest(null);
        }
    }
}

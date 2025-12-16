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
import de.Keyle.MyPet.api.util.EnumSelector;
import de.Keyle.MyPet.entity.MyPet;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagInt;
import de.keyle.knbt.TagList;
import de.keyle.knbt.TagString;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

@Getter
public class MyHorse extends MyPet implements de.Keyle.MyPet.api.entity.types.MyHorse {

    protected ItemStack armor = null;
    protected ItemStack saddle = null;
    protected int variant = 0;

    public MyHorse(MyPetPlayer petOwner) {
        super(petOwner);
    }

    public void setArmor(ItemStack item) {
        if (item != null &&
                !item.getType().name().equals("LEATHER_HORSE_ARMOR") &&
                item.getType() != EnumSelector.find(Material.class, "IRON_BARDING", "IRON_HORSE_ARMOR") &&
                item.getType() != EnumSelector.find(Material.class, "GOLD_BARDING", "GOLDEN_HORSE_ARMOR") &&
                item.getType() != EnumSelector.find(Material.class, "DIAMOND_BARDING", "DIAMOND_HORSE_ARMOR")) {
            return;
        }

        this.armor = item;
        if (this.armor != null) {
            this.armor.setAmount(1);
        }

        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    public boolean hasArmor() {
        return armor != null;
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
        info.getCompoundData().put("Variant", new TagInt(getVariant()));

        // Write horse-specific equipment with string slot names for MC versions before 1.21
        // (which lack EquipmentSlot.BODY and EquipmentSlot.SADDLE)
        if (MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.21") < 0) {
            List<TagCompound> itemList = new ArrayList<>();

            // Preserve any existing equipment from parent
            if (info.containsKey("Equipment")) {
                itemList.addAll((List<TagCompound>) info.getAs("Equipment", TagList.class).getData());
            }

            if (hasArmor()) {
                TagCompound item = MyPetApi.getPlatformHelper().itemStackToCompund(getArmor());
                item.getCompoundData().put("Slot", new TagString("BODY"));
                itemList.add(item);
            }
            if (hasSaddle()) {
                TagCompound item = MyPetApi.getPlatformHelper().itemStackToCompund(getSaddle());
                item.getCompoundData().put("Slot", new TagString("SADDLE"));
                itemList.add(item);
            }
            if (!itemList.isEmpty()) {
                info.put("Equipment", new TagList(itemList));
            }
        }
        return info;
    }

    @Override
    public void readExtendedInfo(TagCompound info) {
        super.readExtendedInfo(info);
        if (info.containsKey("Variant")) {
            setVariant(info.getAs("Variant", TagInt.class).getIntData());
        }
    }

    public void setVariant(int variant) {
        if (variant >= 0 && variant <= 6) {
            this.variant = variant;
        } else if (variant >= 256 && variant <= 262) {
            this.variant = variant;
        } else if (variant >= 512 && variant <= 518) {
            this.variant = variant;
        } else if (variant >= 768 && variant <= 774) {
            this.variant = variant;
        } else if (variant >= 1024 && variant <= 1030) {
            this.variant = variant;
        } else {
            this.variant = 0;
        }
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> entity.getHandle().updateVisuals());
        }
    }

    // MyPetEquipment implementation

    @Override
    public ItemStack[] getEquipment() {
        return new ItemStack[]{armor, saddle};
    }

    @Override
    public ItemStack getEquipment(EquipmentSlot slot) {
        String name = slot.name();
        if (name.equals("BODY")) return armor;
        if (name.equals("SADDLE")) return saddle;
        return null;
    }

    @Override
    public void setEquipment(EquipmentSlot slot, ItemStack item) {
        setEquipmentBySlotName(slot.name(), item);
    }

    @Override
    protected void setEquipmentBySlotName(String slotName, ItemStack item) {
        if (slotName.equals("BODY")) {
            setArmor(item);
        } else if (slotName.equals("SADDLE")) {
            setSaddle(item);
        } else {
            super.setEquipmentBySlotName(slotName, item);
        }
    }

    @Override
    public void dropEquipment() {
        if (status == PetState.Here) {
            getEntity().ifPresent(entity -> {
                if (hasArmor()) {
                    entity.getWorld().dropItem(entity.getLocation(), getArmor());
                }
                if (hasSaddle()) {
                    entity.getWorld().dropItem(entity.getLocation(), getSaddle());
                }
            });
            setArmor(null);
            setSaddle(null);
        }
    }
}
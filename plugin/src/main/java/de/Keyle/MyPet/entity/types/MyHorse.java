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
import org.bukkit.entity.Horse;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

@Getter
public class MyHorse extends MyPet implements de.Keyle.MyPet.api.entity.types.MyHorse {

    protected ItemStack armor = null;
    protected ItemStack saddle = null;
    /**
     * Stored by enum-name for drift-safety. The public {@code int} variant API
     * preserves the legacy packed encoding ({@code color | (style << 8)}) by
     * deriving it from {@link #colorName}/{@link #styleName} on demand.
     */
    protected String colorName = "WHITE";
    protected String styleName = "NONE";

    public MyHorse(MyPetPlayer petOwner) {
        super(petOwner);
    }

    public void setArmor(ItemStack item) {
        if (item != null &&
                item.getType() != Material.LEATHER_HORSE_ARMOR &&
                item.getType() != Material.IRON_HORSE_ARMOR &&
                item.getType() != Material.GOLDEN_HORSE_ARMOR &&
                item.getType() != Material.DIAMOND_HORSE_ARMOR) {
            return;
        }

        this.armor = item;
        if (this.armor != null) {
            this.armor.setAmount(1);
        }

        if (status == PetState.Here) {
            updateVisuals();
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
            updateVisuals();
        }
    }

    public boolean hasSaddle() {
        return saddle != null;
    }

    /**
     * Returns the legacy packed variant: {@code color.ordinal() | style.ordinal() << 8}.
     * Ordinals are computed from the currently resolved {@link Horse.Color}
     * and {@link Horse.Style} enums.
     */
    public int getVariant() {
        try {
            Horse.Color color = resolveColor();
            Horse.Style style = resolveStyle();
            int c = color != null ? color.ordinal() : 0;
            int s = style != null ? style.ordinal() : 0;
            return (c & 0xFF) | ((s & 0xFF) << 8);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    public void setVariant(int variant) {
        int colorIdx = variant & 0xFF;
        int styleIdx = (variant >> 8) & 0xFF;
        try {
            Horse.Color[] colors = Horse.Color.values();
            if (colorIdx >= 0 && colorIdx < colors.length) {
                this.colorName = colors[colorIdx].name();
            }
        } catch (Throwable ignored) {
        }
        try {
            Horse.Style[] styles = Horse.Style.values();
            if (styleIdx >= 0 && styleIdx < styles.length) {
                this.styleName = styles[styleIdx].name();
            }
        } catch (Throwable ignored) {
        }
        if (status == PetState.Here) {
            updateVisuals();
        }
    }

    public Horse.Color resolveColor() {
        try {
            return Horse.Color.valueOf(colorName);
        } catch (Throwable ignored) {
            Horse.Color[] values = Horse.Color.values();
            return values.length > 0 ? values[0] : null;
        }
    }

    public Horse.Style resolveStyle() {
        try {
            return Horse.Style.valueOf(styleName);
        } catch (Throwable ignored) {
            Horse.Style[] values = Horse.Style.values();
            return values.length > 0 ? values[0] : null;
        }
    }

    @Override
    public CompoundBinaryTag writeExtendedInfo() {
        CompoundBinaryTag info = super.writeExtendedInfo();
        CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();
        for (String key : info.keySet()) {
            builder.put(key, info.get(key));
        }
        builder.putString("ColorName", colorName);
        builder.putString("StyleName", styleName);

        // Write horse-specific equipment with string slot names for MC versions before 1.21
        // (which lack EquipmentSlot.BODY and EquipmentSlot.SADDLE)
        if (!MyPetApi.getCompatUtil().minecraftVersionEqualsOrAbove("1.21")) {
            List<BinaryTag> itemList = new ArrayList<>();

            // Preserve any existing equipment from parent
            if (info.keySet().contains("Equipment")) {
                ListBinaryTag existingEquip = info.getList("Equipment");
                for (int i = 0; i < existingEquip.size(); i++) {
                    itemList.add(existingEquip.getCompound(i));
                }
            }

            if (hasArmor()) {
                CompoundBinaryTag item = MyPetApi.getPlatformHelper().itemStackToCompound(getArmor());
                item = item.putString("Slot", "BODY");
                itemList.add(item);
            }
            if (hasSaddle()) {
                CompoundBinaryTag item = MyPetApi.getPlatformHelper().itemStackToCompound(getSaddle());
                item = item.putString("Slot", "SADDLE");
                itemList.add(item);
            }
            if (!itemList.isEmpty()) {
                builder.put("Equipment", ListBinaryTag.listBinaryTag(BinaryTagTypes.COMPOUND, itemList));
            }
        }
        return builder.build();
    }

    @Override
    public void readExtendedInfo(CompoundBinaryTag info) {
        super.readExtendedInfo(info);
        // New format: color/style stored as enum names.
        if (info.keySet().contains("ColorName")) {
            String name = info.getString("ColorName");
            if (name != null && !name.isEmpty()) {
                this.colorName = name;
            }
        }
        if (info.keySet().contains("StyleName")) {
            String name = info.getString("StyleName");
            if (name != null && !name.isEmpty()) {
                this.styleName = name;
            }
        }
        // Legacy format: packed int ordinal.
        if (!info.keySet().contains("ColorName") && !info.keySet().contains("StyleName")
                && info.keySet().contains("Variant")) {
            setVariant(info.getInt("Variant"));
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

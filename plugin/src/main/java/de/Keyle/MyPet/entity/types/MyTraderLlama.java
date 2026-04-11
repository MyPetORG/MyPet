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
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.Material;
import org.bukkit.entity.Llama;
import org.bukkit.inventory.ItemStack;

@Getter
public class MyTraderLlama extends MyPet implements de.Keyle.MyPet.api.entity.types.MyTraderLlama {

    protected ItemStack chest = null;
    protected ItemStack decor = null;
    protected byte horseType = 0;
    /**
     * Stored as {@link Llama.Color} enum name for drift-safety across Paper
     * versions. Public {@code int} variant API preserved for backward compat.
     */
    protected String colorName = "CREAMY";

    public MyTraderLlama(MyPetPlayer petOwner) {
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

    public void setDecor(ItemStack item) {
        if (item != null) {
            switch (item.getType().name()) {
                case "CARPET":
                case "RED_CARPET":
                case "BLACK_CARPET":
                case "CYAN_CARPET":
                case "BLUE_CARPET":
                case "BROWN_CARPET":
                case "GRAY_CARPET":
                case "GREEN_CARPET":
                case "LIME_CARPET":
                case "PINK_CARPET":
                case "ORANGE_CARPET":
                case "MAGENTA_CARPET":
                case "LIGHT_GRAY_CARPET":
                case "LIGHT_BLUE_CARPET":
                case "PURPLE_CARPET":
                case "WHITE_CARPET":
                case "YELLOW_CARPET":
                    break;
                default:
                    return;
            }
        }
        this.decor = item;
        if (this.decor != null) {
            this.decor.setAmount(1);
        }
        if (status == PetState.Here) {
            updateVisuals();
        }
    }

    public boolean hasDecor() {
        return decor != null;
    }

    @Override
    public CompoundBinaryTag writeExtendedInfo() {
        CompoundBinaryTag info = super.writeExtendedInfo();
        info = info.putString("ColorName", colorName);
        if (hasChest()) {
            info = info.put("Chest", MyPetApi.getPlatformHelper().itemStackToCompound(getChest()));
        }
        if (hasDecor()) {
            info = info.put("Decor", MyPetApi.getPlatformHelper().itemStackToCompound(getDecor()));
        }
        return info;
    }

    @Override
    public void readExtendedInfo(CompoundBinaryTag info) {
        super.readExtendedInfo(info);
        if (info.keySet().contains("ColorName")) {
            String name = info.getString("ColorName");
            if (name != null && !name.isEmpty()) {
                this.colorName = name;
            }
        } else if (info.keySet().contains("Variant")) {
            setVariant(info.getInt("Variant"));
        }
        if (info.keySet().contains("Chest")) {
            try {
                // Check if it's a boolean (old format)
                if (info.get("Chest").type().id() == 1) { // BYTE type
                    boolean chest = info.getBoolean("Chest");
                    if (chest) {
                        ItemStack item = new ItemStack(Material.CHEST);
                        setChest(item);
                    }
                } else if (info.get("Chest").type().id() == 10) { // COMPOUND type
                    CompoundBinaryTag itemTag = info.getCompound("Chest");
                    try {
                        ItemStack item = MyPetApi.getPlatformHelper().compoundToItemStack(itemTag);
                        setChest(item);
                    } catch (Exception e) {
                        MyPetApi.getLogger().warning("Could not load Chest item from pet data!");
                    }
                }
            } catch (Exception e) {
                // Ignore if can't determine type
            }
        }
        if (info.keySet().contains("Decor")) {
            try {
                if (info.get("Decor").type().id() == 10) { // COMPOUND type
                    CompoundBinaryTag itemTag = info.getCompound("Decor");
                    try {
                        ItemStack item = MyPetApi.getPlatformHelper().compoundToItemStack(itemTag);
                        setDecor(item);
                    } catch (Exception e) {
                        MyPetApi.getLogger().warning("Could not load Decor item from pet data!");
                    }
                }
            } catch (Exception e) {
                // Ignore if can't determine type
            }
        }
    }

    public int getVariant() {
        try {
            Llama.Color color = resolveColor();
            return color != null ? color.ordinal() : 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    public void setVariant(int variant) {
        try {
            Llama.Color[] values = Llama.Color.values();
            int idx = Math.min(values.length - 1, Math.max(0, variant & 0xFF));
            this.colorName = values[idx].name();
        } catch (Throwable ignored) {
        }
        if (status == PetState.Here) {
            updateVisuals();
        }
    }

    public Llama.Color resolveColor() {
        try {
            return Llama.Color.valueOf(colorName);
        } catch (Throwable ignored) {
            Llama.Color[] values = Llama.Color.values();
            return values.length > 0 ? values[0] : null;
        }
    }
}
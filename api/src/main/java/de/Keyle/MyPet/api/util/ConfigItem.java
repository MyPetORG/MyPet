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

package de.Keyle.MyPet.api.util;

import de.Keyle.MyPet.MyPetApi;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

/**
 * A configured item — typically loaded from config (e.g., {@code RIDE_ITEM},
 * {@code CONTROL_ITEM}, {@code GROW_UP_ITEM}) and compared against items the
 * player is holding.
 */
@Getter
public class ConfigItem {

    protected ItemStack item = null;
    protected DurabilityMode durabilityMode = DurabilityMode.NotUsed;

    public ConfigItem(ItemStack item, DurabilityMode durabilityMode) {
        this.item = item;
        this.durabilityMode = durabilityMode;
    }

    public ConfigItem(String data) {
        if (data.startsWith(".")) {
            // "1.20.5+" item encoding: dot prefix indicates modern component string.
            load(data.replaceFirst("^\\.\\s?", ""));
            return;
        }

        String[] splitData = data.split("\\s+", 2);

        if (splitData.length == 0) {
            return;
        }

        Material material = Material.matchMaterial(splitData[0]);
        if (material == null) {
            MyPetApi.getLogger().warning(splitData[0] + " is not a valid item ID! Please check your configs.");
            return;
        }

        load(splitData[0], splitData.length == 2 ? splitData[1] : null);
    }

    public static ConfigItem createConfigItem(String data) {
        if (data.equalsIgnoreCase("none")) {
            return null;
        }
        return new ConfigItem(data);
    }

    public static ConfigItem createConfigItem(ItemStack item, DurabilityMode durabilityMode) {
        return new ConfigItem(item, durabilityMode);
    }

    public boolean compare(ItemStack compareItem) {
        if (item == null || item.getType() == Material.AIR) {
            return compareItem == null || compareItem.getType() == Material.AIR;
        }
        if (compareItem == null) {
            return false;
        }
        if (item.getType() != compareItem.getType()) {
            return false;
        }
        if (durabilityMode != DurabilityMode.NotUsed) {
            int compareDamage = compareItem.getItemMeta() instanceof Damageable d ? d.getDamage() : 0;
            int itemDamage = item.getItemMeta() instanceof Damageable d2 ? d2.getDamage() : 0;
            switch (durabilityMode) {
                case Bigger:
                    if (compareDamage <= itemDamage) {
                        return false;
                    }
                    break;
                case Smaller:
                    if (compareDamage >= itemDamage) {
                        return false;
                    }
                    break;
                case Equal:
                    if (compareDamage != itemDamage) {
                        return false;
                    }
                    break;
            }
        }
        // If the configured item has metadata (custom name, lore, data components),
        // require the compared item to match metadata too. Without this, a plain
        // wheat would match a renamed "Super Wheat" configured as a special food.
        if (item.hasItemMeta() && !item.isSimilar(compareItem)) {
            return false;
        }
        return true;
    }

    /**
     * Parses a modern-format item string like {@code "minecraft:wheat[custom_name='...']"}
     * via Paper's {@link org.bukkit.inventory.ItemFactory#createItemStack(String)}.
     */
    public void load(String data) {
        try {
            this.item = Bukkit.getItemFactory().createItemStack(data);
        } catch (Throwable e) {
            MyPetApi.getLogger().warning("Error parsing config item: " + e.getMessage() + " caused by: " + data);
        }
    }

    /**
     * Parses a legacy format where the material ID is separate from the optional
     * NBT/components suffix. Delegates to Paper's
     * {@link org.bukkit.inventory.ItemFactory#createItemStack(String)} with the
     * concatenated string, or falls back to a bare material-only ItemStack.
     */
    public void load(String materialId, String data) {
        Material material = Material.matchMaterial(materialId);
        if (material == null) {
            MyPetApi.getLogger().warning(materialId + " is not a valid item ID!");
            return;
        }

        // Default: bare material, no extra data.
        this.item = new ItemStack(material);

        if (data != null && !data.trim().isEmpty()) {
            String trimmed = data.trim();
            // Legacy NBT format ({...}) or modern component format ([...]) — both
            // parseable by ItemFactory.createItemStack() when prefixed with the material.
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                try {
                    this.item = Bukkit.getItemFactory().createItemStack(materialId + trimmed);
                } catch (Throwable e) {
                    MyPetApi.getLogger().warning("Error parsing config item data: " + e.getMessage()
                            + " caused by: " + materialId + " " + trimmed);
                }
            }
        }
    }

    public enum DurabilityMode {
        Smaller, Bigger, NotUsed, Equal
    }
}

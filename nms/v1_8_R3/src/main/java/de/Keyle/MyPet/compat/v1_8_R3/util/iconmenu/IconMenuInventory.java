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

package de.Keyle.MyPet.compat.v1_8_R3.util.iconmenu;

import de.Keyle.MyPet.api.gui.IconMenuItem;
import de.Keyle.MyPet.api.util.Compat;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;

import java.util.List;

/**
 * Version-specific IconMenuInventory for Minecraft v1_8_R3.
 * <p>
 * This class inherits all GUI logic from the API-level {@code IconMenuInventory}
 * and only overrides {@link #createItemStack(IconMenuItem)} to fix issues with 1.8.8 compatibility.
 * <p>
 * Intended behavior:
 * <ul>
 *   <li>No NMS usage</li>
 *   <li>Defer most functionality to the shared parent implementation</li>
 *   <li>Use legacy durability values in createItemStack override</li>
 * </ul>
 */
@Compat("v1_8_R3")
public class IconMenuInventory extends de.Keyle.MyPet.api.gui.IconMenuInventory {

    /**
     * Build a GUI-safe ItemStack from a menu item using only Bukkit APIs.
     */
    @Override
    protected ItemStack createItemStack(IconMenuItem icon) {
        Material material = icon.getMaterial() != null ? icon.getMaterial() : Material.BARRIER;
        int amount = Math.max(1, icon.getAmount());
        short damage = (short) (icon.getData() & 0xFFFF);

        // On 1.8.x, many variants (spawn eggs, stained blocks, skull type) depend on durability (damage) value.
        ItemStack is = new ItemStack(material, amount, damage);

        // Carry over any meta provided by the icon before we mutate it further.
        if (icon.getBukkitMeta() != null) {
            is.setItemMeta(icon.getBukkitMeta());
        }

        ItemMeta meta = is.getItemMeta();
        if (meta == null) return is;

        // Title
        if (icon.getTitle() != null && !icon.getTitle().isEmpty()) {
            meta.setDisplayName(icon.getTitle());
        }

        // Lore
        if (!icon.getLore().isEmpty()) {
            List<String> lore = new ArrayList<>();
            for (String line : icon.getLore()) {
                lore.add((line == null || line.isEmpty()) ? " " : line);
            }
            meta.setLore(lore);
        }

        // Glow: prefer setEnchantmentGlintOverride if present, else dummy enchant hidden
        if (icon.isGlowing()) {
            meta = addGlint(meta);
        }

        // Tooltip flags for compact GUI items
        try {
            meta.addItemFlags(
                    ItemFlag.HIDE_ATTRIBUTES,
                    ItemFlag.HIDE_UNBREAKABLE,
                    ItemFlag.HIDE_DESTROYS,
                    ItemFlag.HIDE_PLACED_ON
            );
        } catch (Throwable ignored) {
            // Some flags not present on older APIs; safe to ignore
        }

        is.setItemMeta(meta);
        return is;
    }

}

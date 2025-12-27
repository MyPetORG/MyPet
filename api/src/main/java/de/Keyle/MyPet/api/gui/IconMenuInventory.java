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

package de.Keyle.MyPet.api.gui;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Default, version-agnostic IconMenu inventory implementation using only the Bukkit API.
 * <p>
 * This class centralizes the shared behavior used across compat modules. It avoids any NMS or CraftBukkit
 * types and should work from legacy servers up to current Spigot/Paper versions. The compat layer can
 * subclass this if needed, but most versions should not require overrides.
 */
public class IconMenuInventory {

    private Inventory bukkitInventory;
    /**
     * Logical size in slots. Set when {@link #open(IconMenu, HumanEntity)} is called.
     */
    @Getter
    private int size;

    /**
     * Opens the menu for the given player and renders all options into a fresh Bukkit inventory.
     * Must be called on the main server thread.
     */
    public void open(IconMenu menu, HumanEntity player) {
        this.size = menu.getSize();
        this.bukkitInventory = Bukkit.createInventory(null, size, menu.getTitle());

        for (int slot = 0; slot < size; slot++) {
            IconMenuItem menuItem = menu.getOption(slot);
            if (menuItem != null) {
                this.bukkitInventory.setItem(slot, createItemStack(menuItem));
            }
        }
        player.openInventory(this.bukkitInventory);
    }

    /**
     * Re-render items in the currently open inventory from the provided model.
     * No-op if the menu is not open.
     */
    public void update(IconMenu menu) {
        if (this.bukkitInventory == null) return;

        for (int slot = 0; slot < size; slot++) {
            IconMenuItem menuItem = menu.getOption(slot);
            if (menuItem != null) {
                this.bukkitInventory.setItem(slot, createItemStack(menuItem));
            } else {
                this.bukkitInventory.setItem(slot, new ItemStack(Material.AIR));
            }
        }
    }

    /**
     * Closes the inventory for all current viewers and clears the backing reference.
     */
    public void close() {
        if (this.bukkitInventory == null) return;

        List<HumanEntity> viewers = new ArrayList<>(getViewers());
        for (HumanEntity viewer : viewers) {
            viewer.closeInventory();
        }
        this.bukkitInventory = null;
    }

    /**
     * Checks if the given Bukkit inventory instance matches this menu's inventory.
     */
    public boolean isMenuInventory(Inventory inv) {
        return this.bukkitInventory != null && this.bukkitInventory.equals(inv);
    }

    /**
     * Current viewers of the backing inventory. Empty if not open.
     */
    public List<HumanEntity> getViewers() {
        if (this.bukkitInventory == null) {
            return Collections.emptyList();
        }
        return this.bukkitInventory.getViewers();
    }

    /**
     * Apply a visual glow effect to the item using Bukkit-safe mechanics.
     * <p>
     * Modern servers (1.20.4+) offer {@code setEnchantmentGlintOverride}. However, this API
     * does not exist on legacy servers, so the default implementation uses a dummy enchant
     * and hides it. Subclasses may override this method to use the modern API when available.
     *
     * @param meta base {@link ItemMeta} to modify
     * @return modified {@link ItemMeta} with glow applied
     */
    protected ItemMeta addGlint(ItemMeta meta) {
        // Legacy-safe glow technique: add dummy enchant and hide it from view
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.LUCK, 1, true);
        return meta;
    }

    /**
     * Build a GUI-safe ItemStack from a menu item using only Bukkit APIs.
     */
    protected ItemStack createItemStack(IconMenuItem icon) {
        // Defensive defaults
        ItemStack is = new ItemStack(icon.getMaterial() != null ? icon.getMaterial() : Material.BARRIER,
                Math.max(1, icon.getAmount()));
        is.setItemMeta(icon.getBukkitMeta());
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

        // Clear attribute modifiers where supported
        try {
            if (meta.hasAttributeModifiers()) {
                // Wipe all modifiers to avoid referencing enum constants not present on legacy
                meta.setAttributeModifiers(null);
            }
        } catch (Throwable ignored) {
            // Older APIs do not expose attribute modifiers; nothing to do
        }

        is.setItemMeta(meta);
        return is;
    }
}
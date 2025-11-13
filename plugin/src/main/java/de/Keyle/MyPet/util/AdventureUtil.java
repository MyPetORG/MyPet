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

package de.Keyle.MyPet.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for converting and building Kyori Adventure components.
 * Used for migration from RawMessage library.
 */
public class AdventureUtil {

    /**
     * Maps MessageColor-equivalent colors to Kyori NamedTextColor
     */
    public static NamedTextColor getColor(String colorName) {
        if (colorName == null) return null;
        
        return switch (colorName.toUpperCase()) {
            case "BLACK" -> NamedTextColor.BLACK;
            case "DARK_BLUE" -> NamedTextColor.DARK_BLUE;
            case "DARK_GREEN" -> NamedTextColor.DARK_GREEN;
            case "DARK_AQUA" -> NamedTextColor.DARK_AQUA;
            case "DARK_RED" -> NamedTextColor.DARK_RED;
            case "DARK_PURPLE" -> NamedTextColor.DARK_PURPLE;
            case "GOLD" -> NamedTextColor.GOLD;
            case "GRAY", "GREY" -> NamedTextColor.GRAY;
            case "DARK_GRAY", "DARK_GREY" -> NamedTextColor.DARK_GRAY;
            case "BLUE" -> NamedTextColor.BLUE;
            case "GREEN" -> NamedTextColor.GREEN;
            case "AQUA" -> NamedTextColor.AQUA;
            case "RED" -> NamedTextColor.RED;
            case "LIGHT_PURPLE" -> NamedTextColor.LIGHT_PURPLE;
            case "YELLOW" -> NamedTextColor.YELLOW;
            case "WHITE" -> NamedTextColor.WHITE;
            default -> null;
        };
    }

    /**
     * Creates a hover event for displaying an item
     */
    public static HoverEvent<HoverEvent.ShowItem> createItemHover(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return null;
        }

        try {
            // Create basic hover item with Material and amount
            ItemStack displayItem = itemStack.clone();
            
            // Use Kyori's ItemStack integration via HoverEvent.ShowItem
            return HoverEvent.showItem(
                net.kyori.adventure.key.Key.key("minecraft", itemStack.getType().getKey().getKey()),
                itemStack.getAmount(),
                BinaryTagHolder.binaryTagHolder(itemStack.getItemMeta() != null ?
                    serializeItemMeta(itemStack) : "{}")
            );
        } catch (Exception e) {
            // Fallback to simple hover
            return null;
        }
    }

    /**
     * Serializes ItemMeta to NBT for hover display
     */
    private static String serializeItemMeta(ItemStack itemStack) {
        // This is a simplified version - in production, proper NBT serialization would be needed
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return "{}";
        }

        // For now, return empty NBT - actual implementation would require full NBT serialization
        return "{}";
    }

    /**
     * Creates a component from text with optional color
     */
    public static Component text(String text, NamedTextColor color) {
        if (color != null) {
            return Component.text(text).color(color);
        }
        return Component.text(text);
    }

    /**
     * Creates a clickable component
     */
    public static Component clickable(String text, String command, NamedTextColor color) {
        TextComponent clickComponent = Component.text(text).clickEvent(ClickEvent.runCommand(command));
        if (color != null) {
            return clickComponent.color(color);
        }
        return clickComponent;
    }

    /**
     * Creates a component with hover text
     */
    public static Component hoverable(String text, Component hoverText, NamedTextColor color) {
        TextComponent hoverComponent = Component.text(text).hoverEvent(HoverEvent.showText(hoverText));
        if (color != null) {
            return hoverComponent.color(color);
        }
        return hoverComponent;
    }

    /**
     * Creates a translatable component
     */
    public static Component translatable(String key, Component... args) {
        return Component.translatable(key, List.of(args));
    }
}

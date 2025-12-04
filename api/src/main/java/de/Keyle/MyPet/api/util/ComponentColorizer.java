/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2025 Keyle
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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;

import java.util.HashMap;
import java.util.Map;

/**
 * Adventure Component-based color parser for MyPet's legacy color code formats.
 * Supports: <color>, <code> patterns
 * <p>
 * This is the modern replacement for Colorizer.setColors() that returns Components
 * instead of ChatColor-formatted strings.
 */
public class ComponentColorizer {


    // Map ChatColor names to Adventure NamedTextColor
    private static final Map<String, TextColor> COLOR_MAP = new HashMap<>();
    private static final Map<Character, TextDecoration> DECORATION_MAP = new HashMap<>();

    static {
        // Build color mapping
        COLOR_MAP.put("black", NamedTextColor.BLACK);
        COLOR_MAP.put("dark_blue", NamedTextColor.DARK_BLUE);
        COLOR_MAP.put("darkblue", NamedTextColor.DARK_BLUE);
        COLOR_MAP.put("dark_green", NamedTextColor.DARK_GREEN);
        COLOR_MAP.put("darkgreen", NamedTextColor.DARK_GREEN);
        COLOR_MAP.put("dark_aqua", NamedTextColor.DARK_AQUA);
        COLOR_MAP.put("darkaqua", NamedTextColor.DARK_AQUA);
        COLOR_MAP.put("dark_red", NamedTextColor.DARK_RED);
        COLOR_MAP.put("darkred", NamedTextColor.DARK_RED);
        COLOR_MAP.put("dark_purple", NamedTextColor.DARK_PURPLE);
        COLOR_MAP.put("darkpurple", NamedTextColor.DARK_PURPLE);
        COLOR_MAP.put("gold", NamedTextColor.GOLD);
        COLOR_MAP.put("gray", NamedTextColor.GRAY);
        COLOR_MAP.put("grey", NamedTextColor.GRAY);
        COLOR_MAP.put("dark_gray", NamedTextColor.DARK_GRAY);
        COLOR_MAP.put("darkgray", NamedTextColor.DARK_GRAY);
        COLOR_MAP.put("dark_grey", NamedTextColor.DARK_GRAY);
        COLOR_MAP.put("darkgrey", NamedTextColor.DARK_GRAY);
        COLOR_MAP.put("blue", NamedTextColor.BLUE);
        COLOR_MAP.put("green", NamedTextColor.GREEN);
        COLOR_MAP.put("aqua", NamedTextColor.AQUA);
        COLOR_MAP.put("cyan", NamedTextColor.AQUA);
        COLOR_MAP.put("red", NamedTextColor.RED);
        COLOR_MAP.put("light_purple", NamedTextColor.LIGHT_PURPLE);
        COLOR_MAP.put("lightpurple", NamedTextColor.LIGHT_PURPLE);
        COLOR_MAP.put("yellow", NamedTextColor.YELLOW);
        COLOR_MAP.put("white", NamedTextColor.WHITE);

        // Code mappings (& or § followed by code)
        COLOR_MAP.put("0", NamedTextColor.BLACK);
        COLOR_MAP.put("1", NamedTextColor.DARK_BLUE);
        COLOR_MAP.put("2", NamedTextColor.DARK_GREEN);
        COLOR_MAP.put("3", NamedTextColor.DARK_AQUA);
        COLOR_MAP.put("4", NamedTextColor.DARK_RED);
        COLOR_MAP.put("5", NamedTextColor.DARK_PURPLE);
        COLOR_MAP.put("6", NamedTextColor.GOLD);
        COLOR_MAP.put("7", NamedTextColor.GRAY);
        COLOR_MAP.put("8", NamedTextColor.DARK_GRAY);
        COLOR_MAP.put("9", NamedTextColor.BLUE);
        COLOR_MAP.put("a", NamedTextColor.GREEN);
        COLOR_MAP.put("b", NamedTextColor.AQUA);
        COLOR_MAP.put("c", NamedTextColor.RED);
        COLOR_MAP.put("d", NamedTextColor.LIGHT_PURPLE);
        COLOR_MAP.put("e", NamedTextColor.YELLOW);
        COLOR_MAP.put("f", NamedTextColor.WHITE);

        // Decoration mappings
        DECORATION_MAP.put('k', TextDecoration.OBFUSCATED);
        DECORATION_MAP.put('l', TextDecoration.BOLD);
        DECORATION_MAP.put('m', TextDecoration.STRIKETHROUGH);
        DECORATION_MAP.put('n', TextDecoration.UNDERLINED);
        DECORATION_MAP.put('o', TextDecoration.ITALIC);
    }

    /**
     * Parses a text string with MyPet color codes into an Adventure Component.
     * Supports: <colorname>, <code>, &code, <reset>, <r>
     *
     * @param text Text with color codes
     * @return Parsed Component with colors applied
     */
    public static Component parseToComponent(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        // Fast path for plain text (no color tags)
        if (!text.contains("<")) {
            return Component.text(text);
        }

        TextComponent.Builder builder = Component.text();
        TextComponent.Builder currentSegment = Component.text();
        StringBuilder currentText = new StringBuilder();

        TextColor currentColor = null;
        Map<TextDecoration, Boolean> currentDecorations = new HashMap<>();

        int i = 0;
        while (i < text.length()) {
            // Check for <color> pattern
            if (text.charAt(i) == '<') {
                int closeIndex = text.indexOf('>', i);
                if (closeIndex != -1) {
                    String tag = text.substring(i + 1, closeIndex).toLowerCase();

                    // Flush current segment if it has text
                    if (currentText.length() > 0) {
                        TextComponent segment = Component.text(currentText.toString());
                        if (currentColor != null) {
                            segment = segment.color(currentColor);
                        }
                        for (Map.Entry<TextDecoration, Boolean> entry : currentDecorations.entrySet()) {
                            segment = segment.decoration(entry.getKey(), entry.getValue());
                        }
                        builder.append(segment);
                        currentText = new StringBuilder();
                    }

                    // Handle reset
                    if (tag.equals("reset") || tag.equals("r")) {
                        currentColor = null;
                        currentDecorations.clear();
                    }
                    // Handle color
                    else if (COLOR_MAP.containsKey(tag)) {
                        currentColor = COLOR_MAP.get(tag);
                    }
                    // Handle decorations
                    else if (tag.length() == 1) {
                        Character code = tag.charAt(0);
                        if (DECORATION_MAP.containsKey(code)) {
                            currentDecorations.put(DECORATION_MAP.get(code), true);
                        } else if (COLOR_MAP.containsKey(tag)) {
                            currentColor = COLOR_MAP.get(tag);
                        }
                    }

                    i = closeIndex + 1;
                    continue;
                }
            }

            // Regular character
            currentText.append(text.charAt(i));
            i++;
        }

        // Flush final segment
        if (currentText.length() > 0) {
            TextComponent segment = Component.text(currentText.toString());
            if (currentColor != null) {
                segment = segment.color(currentColor);
            }
            for (Map.Entry<TextDecoration, Boolean> entry : currentDecorations.entrySet()) {
                segment = segment.decoration(entry.getKey(), entry.getValue());
            }
            builder.append(segment);
        }

        Component result = builder.build();

        // Return empty component if nothing was built
        return result.children().isEmpty() && (result instanceof TextComponent && ((TextComponent) result).content().isEmpty())
                ? Component.text(text) // Fallback to plain text
                : result;
    }

    /**
     * Strips all color codes from text, returning plain text
     *
     * @param text Text with color codes
     * @return Plain text without codes
     */
    public static String stripColors(String text) {
        if (text == null) {
            return null;
        }

        // Remove <tag> patterns
        text = text.replaceAll("(?i)<[a-z_0-9]+>", "");
        // Remove § codes if any
        text = ChatColor.stripColor(text);

        return text;
    }
}

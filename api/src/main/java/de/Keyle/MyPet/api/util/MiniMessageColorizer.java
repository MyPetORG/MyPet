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
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.regex.Pattern;

/**
 * MiniMessage-based color parser for MyPet with automatic fallback to legacy color codes.
 * <p>
 * This parser supports modern MiniMessage formatting:
 * - Gradients: <gradient:color1:color2>text</gradient>
 * - Rainbow: <rainbow>text</rainbow>
 * - Hover: <hover:show_text:'tooltip'>text</hover>
 * - Click: <click:run_command:'/command'>text</click>
 * - Hex colors: <color:#ff0000>text</color>
 * <p>
 * And maintains backward compatibility with legacy formats:
 * - Color names: <red>, <gold>, <aqua>
 * - Color codes: <c>, <6>
 * - Decorations: <l> (bold), <o> (italic), etc.
 * <p>
 * The parser automatically detects which format is being used and chooses
 * the appropriate parsing strategy for optimal performance.
 */
public class MiniMessageColorizer {

    // MiniMessage instance with all standard tags enabled
    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder()
            .tags(TagResolver.resolver(
                    StandardTags.color(),
                    StandardTags.decorations(),
                    StandardTags.gradient(),
                    StandardTags.rainbow(),
                    StandardTags.reset(),
                    StandardTags.transition(),
                    StandardTags.insertion(),
                    StandardTags.clickEvent(),
                    StandardTags.hoverEvent(),
                    StandardTags.keybind(),
                    StandardTags.translatable(),
                    StandardTags.font(),
                    StandardTags.newline(),
                    StandardTags.selector(),
                    StandardTags.score()
            ))
            .build();

    // Strict MiniMessage instance (no legacy color code support)
    private static final MiniMessage MINI_MESSAGE_STRICT = MiniMessage.builder()
            .tags(TagResolver.resolver(
                    StandardTags.color(),
                    StandardTags.decorations(),
                    StandardTags.gradient(),
                    StandardTags.rainbow(),
                    StandardTags.reset(),
                    StandardTags.transition(),
                    StandardTags.insertion(),
                    StandardTags.clickEvent(),
                    StandardTags.hoverEvent(),
                    StandardTags.keybind(),
                    StandardTags.translatable(),
                    StandardTags.font(),
                    StandardTags.newline(),
                    StandardTags.selector(),
                    StandardTags.score()
            ))
            .build();

    // Pattern to detect MiniMessage-specific tags (not legacy color codes)
    private static final Pattern MINIMESSAGE_PATTERN = Pattern.compile(
            "(?i)<(?:gradient|rainbow|hover|click|color:#|transition|insertion|key|translatable|font|selector|score)(?:[^>]*>|>)"
    );

    /**
     * Parses text with automatic format detection.
     * Uses MiniMessage for modern tags, falls back to ComponentColorizer for legacy codes.
     *
     * @param text Text to parse (may contain MiniMessage or legacy color codes)
     * @return Parsed Component with all formatting applied
     */
    public static Component parseToComponent(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        // Fast path: plain text with no formatting
        if (!text.contains("<")) {
            return Component.text(text);
        }

        // Detect if text contains MiniMessage-specific tags
        if (hasMiniMessageTags(text)) {
            // Use MiniMessage parser for advanced formatting
            try {
                return MINI_MESSAGE.deserialize(text);
            } catch (Exception e) {
                // If MiniMessage parsing fails, fall back to legacy parser
                return ComponentColorizer.parseToComponent(text);
            }
        } else {
            // Use legacy ComponentColorizer for backward compatibility
            return ComponentColorizer.parseToComponent(text);
        }
    }

    /**
     * Parses text using strict MiniMessage format only.
     * Does not fall back to legacy color codes. Use this when you want to ensure
     * only MiniMessage tags are processed.
     *
     * @param text Text with MiniMessage tags
     * @return Parsed Component
     */
    public static Component parseMiniMessageOnly(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        try {
            return MINI_MESSAGE_STRICT.deserialize(text);
        } catch (Exception e) {
            // Return plain text on parse error
            return Component.text(text);
        }
    }

    /**
     * Parses text using legacy color codes only.
     * This is equivalent to ComponentColorizer.parseToComponent() but provided
     * for consistency in the API.
     *
     * @param text Text with legacy color codes (<red>, &c, etc.)
     * @return Parsed Component
     */
    public static Component parseLegacyOnly(String text) {
        return ComponentColorizer.parseToComponent(text);
    }

    /**
     * Checks if text contains MiniMessage-specific tags.
     * This is used to determine which parser to use for optimal performance.
     *
     * @param text Text to check
     * @return true if text contains MiniMessage tags, false otherwise
     */
    public static boolean hasMiniMessageTags(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        return MINIMESSAGE_PATTERN.matcher(text).find();
    }

    /**
     * Strips all formatting from text, returning plain text.
     * Handles both MiniMessage and legacy color codes.
     *
     * @param text Text with formatting
     * @return Plain text without any formatting
     */
    public static String stripFormatting(String text) {
        if (text == null) {
            return null;
        }

        // First, try to parse and serialize to plain text
        try {
            Component component = parseToComponent(text);
            return PlainTextComponentSerializer.plainText().serialize(component);
        } catch (Exception e) {
            // Fallback: use ComponentColorizer's strip method
            return ComponentColorizer.stripColors(text);
        }
    }

    /**
     * Gets the configured MiniMessage instance.
     * This instance includes all standard tags and supports legacy color codes.
     *
     * @return MiniMessage instance
     */
    public static MiniMessage getMiniMessage() {
        return MINI_MESSAGE;
    }

    /**
     * Gets the strict MiniMessage instance.
     * This instance only supports MiniMessage tags, not legacy color codes.
     *
     * @return Strict MiniMessage instance
     */
    public static MiniMessage getStrictMiniMessage() {
        return MINI_MESSAGE_STRICT;
    }

    // ========== Convenience Methods ==========

    /**
     * Creates a gradient Component from color1 to color2.
     *
     * @param text   Text to apply gradient to
     * @param color1 Start color (hex like "ff0000" or name like "red")
     * @param color2 End color (hex like "00ff00" or name like "green")
     * @return Component with gradient applied
     */
    public static Component gradient(String text, String color1, String color2) {
        String formatted = String.format("<gradient:%s:%s>%s</gradient>", color1, color2, text);
        return parseMiniMessageOnly(formatted);
    }

    /**
     * Creates a rainbow Component.
     *
     * @param text Text to apply rainbow effect to
     * @return Component with rainbow colors
     */
    public static Component rainbow(String text) {
        String formatted = String.format("<rainbow>%s</rainbow>", text);
        return parseMiniMessageOnly(formatted);
    }

    /**
     * Creates a Component with hover text.
     *
     * @param text      Main text
     * @param hoverText Text to show on hover
     * @return Component with hover event
     */
    public static Component withHover(String text, String hoverText) {
        String formatted = String.format("<hover:show_text:'%s'>%s</hover>",
                hoverText.replace("'", "\\'"), text);
        return parseMiniMessageOnly(formatted);
    }

    /**
     * Creates a Component with click command.
     *
     * @param text    Main text
     * @param command Command to run (without leading slash)
     * @return Component with click event
     */
    public static Component withClick(String text, String command) {
        String formatted = String.format("<click:run_command:'/%s'>%s</click>",
                command, text);
        return parseMiniMessageOnly(formatted);
    }

    /**
     * Creates a Component with hex color.
     *
     * @param text     Text to color
     * @param hexColor Hex color without # (e.g., "ff0000")
     * @return Component with hex color
     */
    public static Component hexColor(String text, String hexColor) {
        String formatted = String.format("<color:#%s>%s</color>", hexColor, text);
        return parseMiniMessageOnly(formatted);
    }
}

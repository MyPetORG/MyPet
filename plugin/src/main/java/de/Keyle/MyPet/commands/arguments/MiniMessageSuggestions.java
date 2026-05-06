/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
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

package de.Keyle.MyPet.commands.arguments;

import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.Keyle.MyPet.api.player.Permissions;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Provides MiniMessage tag completion suggestions for pet name arguments
 * in Brigadier commands.
 *
 * <p>When a player is typing a pet name and opens an angle bracket ({@code <}),
 * this class offers context-aware tab completions for MiniMessage formatting
 * tags. The available tags are split into two permission-gated categories:</p>
 * <ul>
 *     <li><b>Color tags</b> (permission {@code MyPet.command.name.color}) &mdash;
 *         named colors ({@code black}, {@code red}, etc.), {@code color:},
 *         {@code reset}, {@code gradient:}, and {@code rainbow}.</li>
 *     <li><b>Decoration tags</b> (permission {@code MyPet.command.name.format}) &mdash;
 *         {@code bold}, {@code italic}, {@code underlined},
 *         {@code strikethrough}, {@code obfuscated}, and their closing
 *         counterparts (e.g. {@code /bold}).</li>
 * </ul>
 *
 * <p>Only {@link Player} senders are checked for permissions; non-player
 * senders (e.g. console) receive no suggestions.</p>
 *
 * <p>This class is not instantiable; all functionality is accessed through
 * the static {@link #suggest(SuggestionsBuilder, CommandSender)} method.</p>
 */
public final class MiniMessageSuggestions {

    /**
     * Immutable list of MiniMessage color tag names offered when the sender
     * holds the {@code MyPet.command.name.color} permission. Tags ending
     * with {@code :} (e.g. {@code color:}, {@code gradient:}) expect an
     * additional value and are suggested without a closing {@code >}.
     */
    private static final List<String> COLOR_TAGS = List.of(
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
            "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple",
            "yellow", "white", "color:", "reset", "gradient:", "rainbow"
    );

    /**
     * Immutable list of MiniMessage decoration tag names (and their closing
     * variants) offered when the sender holds the
     * {@code MyPet.command.name.format} permission.
     */
    private static final List<String> DECORATION_TAGS = List.of(
            "bold", "italic", "underlined", "strikethrough", "obfuscated",
            "/bold", "/italic", "/underlined", "/strikethrough", "/obfuscated"
    );

    /** Private constructor to prevent instantiation of this utility class. */
    private MiniMessageSuggestions() {}

    /**
     * Adds MiniMessage tag suggestions to the given {@link SuggestionsBuilder}
     * if the player is currently typing inside an unclosed {@code <} tag.
     *
     * <p>The method inspects the remaining (unmatched) input in the builder
     * to determine whether the cursor is inside an open tag. If the last
     * {@code <} has no corresponding {@code >} after it, the text after the
     * {@code <} is treated as a partial tag name and matched against the
     * available tags.</p>
     *
     * <p>Suggestions are permission-gated:</p>
     * <ul>
     *     <li>Decoration tags are only suggested if the sender has
     *         {@code MyPet.command.name.format}.</li>
     *     <li>Color tags are only suggested if the sender has
     *         {@code MyPet.command.name.color}.</li>
     * </ul>
     *
     * <p>Tags that require a value (those ending with {@code :}, such as
     * {@code color:} or {@code gradient:}) are suggested without a closing
     * {@code >} so the player can continue typing the value. All other tags
     * are suggested with the closing {@code >} appended.</p>
     *
     * @param builder the Brigadier suggestions builder to populate
     * @param sender  the command sender; must be a {@link Player} for
     *                permission checks to succeed, otherwise no suggestions
     *                are added
     */
    public static void suggest(SuggestionsBuilder builder, CommandSender sender) {
        String input = builder.getRemaining();
        int lastOpen = input.lastIndexOf('<');
        if (lastOpen < 0 || input.indexOf('>', lastOpen) >= 0) {
            return;
        }

        String prefix = input.substring(0, lastOpen + 1);
        String partial = input.substring(lastOpen + 1).toLowerCase();
        Player p = sender instanceof Player pl ? pl : null;

        if (p != null && Permissions.has(p, "MyPet.command.name.format")) {
            for (String tag : DECORATION_TAGS) {
                if (tag.startsWith(partial)) {
                    builder.suggest(prefix + tag + ">");
                }
            }
        }
        if (p != null && Permissions.has(p, "MyPet.command.name.color")) {
            for (String tag : COLOR_TAGS) {
                if (tag.startsWith(partial)) {
                    builder.suggest(prefix + tag + (tag.endsWith(":") ? "" : ">"));
                }
            }
        }
    }
}

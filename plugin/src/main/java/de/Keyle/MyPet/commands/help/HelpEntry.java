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

package de.Keyle.MyPet.commands.help;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Represents a single entry in the {@code /mypet help} command output.
 * Each entry maps a command to its translated description, category, display order,
 * and an optional visibility predicate that controls whether the entry is shown to a given player.
 *
 * <p>Entries are registered via {@link HelpRegistry#register(HelpEntry)} and are typically
 * created during command registration in {@code BuiltInCommands.register()}.
 *
 * @param translationKey the translation key used to look up the localized help description
 *                       for this command (e.g., {@code "Name.Help.Description"})
 * @param command        the command syntax string displayed to the player (e.g., {@code "/mypet name <name>"})
 * @param category       the {@link CommandCategory} this entry belongs to, used for grouping in help output
 * @param order          the sort order within help listings; lower values appear first
 * @param visibleTo      an optional predicate that determines whether a player can see this entry;
 *                       if {@code null}, the entry is visible to all players
 */
public record HelpEntry(
        String translationKey,
        String command,
        CommandCategory category,
        int order,
        @Nullable Predicate<Player> visibleTo
) {
    /**
     * Checks whether this help entry should be visible to the given player.
     * If no visibility predicate was provided (i.e., {@link #visibleTo} is {@code null}),
     * the entry is considered visible to everyone.
     *
     * @param player the player to check visibility for
     * @return {@code true} if the entry is visible to the player, {@code false} otherwise
     */
    public boolean isVisibleTo(Player player) {
        return visibleTo == null || visibleTo.test(player);
    }
}

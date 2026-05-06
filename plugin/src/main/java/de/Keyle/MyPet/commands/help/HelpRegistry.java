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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Central registry for {@link HelpEntry} instances that power the {@code /mypet help} command.
 *
 * <p>Commands register their help entries here during plugin startup. The registry provides
 * various retrieval methods that return entries sorted by their {@linkplain HelpEntry#order() order}
 * value, with optional filtering by {@link CommandCategory} and player visibility.
 *
 * <p>All retrieval methods return immutable snapshot lists sorted in ascending order.
 *
 * @see HelpEntry
 * @see CommandCategory
 */
public class HelpRegistry {

    private final List<HelpEntry> entries = new ArrayList<>();

    /**
     * Registers a new help entry in the registry. Entries are stored in insertion order
     * and sorted on retrieval.
     *
     * @param entry the help entry to register; must not be {@code null}
     */
    public void register(HelpEntry entry) {
        entries.add(entry);
    }

    /**
     * Returns all registered help entries, sorted by {@linkplain HelpEntry#order() order} ascending.
     *
     * @return an immutable list of all entries sorted by display order
     */
    public List<HelpEntry> getEntries() {
        return entries.stream()
                .sorted(Comparator.comparingInt(HelpEntry::order))
                .toList();
    }

    /**
     * Returns all help entries that are visible to the given player, sorted by
     * {@linkplain HelpEntry#order() order} ascending. Entries without a visibility predicate
     * are included for all players.
     *
     * @param player the player to check visibility against
     * @return an immutable list of visible entries sorted by display order
     * @see HelpEntry#isVisibleTo(Player)
     */
    public List<HelpEntry> getEntriesVisibleTo(Player player) {
        return entries.stream()
                .filter(e -> e.isVisibleTo(player))
                .sorted(Comparator.comparingInt(HelpEntry::order))
                .toList();
    }

    /**
     * Returns all help entries belonging to the specified category, sorted by
     * {@linkplain HelpEntry#order() order} ascending. No visibility filtering is applied.
     *
     * @param category the category to filter by
     * @return an immutable list of matching entries sorted by display order
     */
    public List<HelpEntry> getEntriesByCategory(CommandCategory category) {
        return entries.stream()
                .filter(e -> e.category() == category)
                .sorted(Comparator.comparingInt(HelpEntry::order))
                .toList();
    }

    /**
     * Returns all help entries belonging to the specified category that are also visible
     * to the given player, sorted by {@linkplain HelpEntry#order() order} ascending.
     * This is the primary method used to build per-player, per-category help output.
     *
     * @param category the category to filter by
     * @param player   the player to check visibility against
     * @return an immutable list of matching, visible entries sorted by display order
     * @see HelpEntry#isVisibleTo(Player)
     */
    public List<HelpEntry> getEntriesByCategoryVisibleTo(CommandCategory category, Player player) {
        return entries.stream()
                .filter(e -> e.category() == category && e.isVisibleTo(player))
                .sorted(Comparator.comparingInt(HelpEntry::order))
                .toList();
    }
}

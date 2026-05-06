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

/**
 * Categories used to group commands in the {@code /mypet help} output.
 * Each command's {@link HelpEntry} is assigned a category so that related commands
 * are displayed together in the help listing.
 */
public enum CommandCategory {
    /**
     * Commands related to pet management, such as naming, releasing,
     * switching pets, and viewing pet info.
     */
    PET("Pet"),

    /**
     * Commands related to pet skills, such as viewing the skill tree
     * or managing individual skill behaviors.
     */
    SKILLS("Skills"),

    /**
     * Administrative commands for server operators, such as reloading
     * configuration, managing other players' pets, or debugging.
     */
    ADMIN("Admin");

    private final String displayName;

    /**
     * Creates a new command category with the given human-readable display name.
     *
     * @param displayName the display name shown in help output
     */
    CommandCategory(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the human-readable display name for this category,
     * suitable for use in help output sent to players.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }
}

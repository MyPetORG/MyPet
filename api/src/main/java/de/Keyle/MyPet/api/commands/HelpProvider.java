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

package de.Keyle.MyPet.api.commands;

import org.bukkit.entity.Player;

public interface HelpProvider {

    /** Translation key for help text (e.g. "Message.Command.Help.Call"). Return null to hide from help. */
    default String getHelpTranslationKey() {
        return null;
    }

    /** Command string shown in help (e.g. "/petcall"). Return null to hide from help. */
    default String getHelpCommand() {
        return null;
    }

    /** Whether this command should be visible to the given player in help. */
    default boolean isVisibleTo(Player player) {
        return true;
    }

    /** Display order in the help list (lower values appear first). */
    default int getHelpOrder() {
        return Integer.MAX_VALUE;
    }

    /** The category this command belongs to in the help listing. */
    default CommandCategory getHelpCategory() {
        return CommandCategory.PET;
    }

    /** Direct help description, used when no translation key is set. */
    default String getHelpDescription() {
        return null;
    }
}

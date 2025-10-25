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

package de.Keyle.MyPet.api.entity.leashing;

import de.Keyle.MyPet.api.util.configuration.settings.Settings;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public interface LeashFlag {

    boolean check(Player player, LivingEntity entity, double damage, Settings settings);

    default String getMissingMessage(Player player, LivingEntity entity, double damage, Settings settings) {
        return null;
    }

    default boolean ignoredByHelper() {
        return false;
    }

    static String getMessagePrefix(boolean right) {
        if (right) {
            return "" + ChatColor.GREEN + ChatColor.BOLD + "✔ " + ChatColor.RESET;
        } else {
            return "" + ChatColor.RED + ChatColor.BOLD + "✘ " + ChatColor.RESET;
        }
    }
}

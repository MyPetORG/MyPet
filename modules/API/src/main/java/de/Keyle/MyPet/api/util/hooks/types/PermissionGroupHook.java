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

package de.Keyle.MyPet.api.util.hooks.types;

import de.Keyle.MyPet.api.util.hooks.PluginHook;
import org.bukkit.entity.Player;

/**
 * This interface defines that the hook handles party checks
 */
public interface PermissionGroupHook extends PluginHook {

    /**
     * Returns if a player is in a group (Vault compatible)
     *
     * @param player checked player
     * @return if a player is in a group
     */
    boolean isInGroup(Player player, String group);

    /**
     * Returns if a player is in a group in a world (Vault compatible)
     *
     * @param player checked player
     * @param world  world to check in
     * @return if a player is in a group in a world
     */
    boolean isInGroup(Player player, String group, String world);
}
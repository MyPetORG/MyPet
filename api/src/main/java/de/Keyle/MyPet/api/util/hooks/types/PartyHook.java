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

import java.util.List;

/**
 * This interface defines that the hook handles party checks
 */
public interface PartyHook extends PluginHook {
    /**
     * Returns if a player is in a party (Ancient, mcMMO, Heroes)
     *
     * @param player checked player
     * @return if a player is in a party
     */
    boolean isInParty(Player player);

    /**
     * Returns all members of a party if the player is in one (Ancient, mcMMO, Heroes)
     * @param player members of the party
     * @return all members of the party
     */
    List<Player> getPartyMembers(Player player);
}
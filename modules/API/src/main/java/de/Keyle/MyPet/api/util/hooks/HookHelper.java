/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2020 Keyle
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

package de.Keyle.MyPet.api.util.hooks;

import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.hooks.types.EconomyHook;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * The {@link HookHelper} is a wrapper for the hooks of the default functionality this plugin needs from other plugins.
 */
public abstract class HookHelper {

    /**
     * Return if a player can hurt another player and if needed vice versa
     *
     * @param attacker  attacking player
     * @param victim    attacked player
     * @param viceversa when true check if the attacked can attack the attacker
     * @return if the attacker can hurt the attacked (and optionally vice versa)
     */
    public abstract boolean canHurt(Player attacker, Player victim, boolean viceversa);

    /**
     * Return if a player can hurt another player.
     *
     * @param attacker attacking player
     * @param victim   attacked player
     * @return if the attacker can hurt the attacked player
     */
    public abstract boolean canHurt(Player attacker, Player victim);

    /**
     * Return if a player can hurt a specific entity.
     *
     * @param attacker attacking player
     * @param victim   attacked entity
     * @return if the attacker can hurt the entity
     */
    public abstract boolean canHurt(Player attacker, Entity victim);

    /**
     * Return if a MyPet player is in any type of arena (Minigames, Survival Games, PvP Arena, etc.)
     *
     * @param player the MyPet player
     * @return if player is in arena
     */
    public abstract boolean isPetAllowed(MyPetPlayer player);

    /**
     * Returns if a player can fly a pet at a certain location
     *
     * @param location checked location
     * @return if a player can fly the pet
     */
    public abstract boolean canMyPetFlyAt(Location location);

    /**
     * Returns if a player is in a party (Ancient, mcMMO, Heroes)
     *
     * @param player checked player
     * @return if a player is in a party
     */
    public abstract boolean isInParty(Player player);

    /**
     * Returns all members of a party if the player is in one (Ancient, mcMMO, Heroes)
     *
     * @param player members of the party
     * @return all members of the party
     */
    public abstract List<Player> getPartyMembers(Player player);

    /**
     * Returns wether the player is vanished or not
     *
     * @param player the player to be checked
     * @return wether the player is vanished
     */
    public abstract boolean isVanished(Player player);

    /**
     * Returns the Vault economy hook
     *
     * @return the Vault economy hook
     */
    public abstract EconomyHook getEconomy();

    /**
     * Returns if the Vault economy hook is enabled
     *
     * @return true if enabled
     */
    public abstract boolean isEconomyEnabled();
}
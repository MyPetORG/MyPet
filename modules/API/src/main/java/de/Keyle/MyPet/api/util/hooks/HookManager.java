/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;

public abstract class HookManager {
    public abstract boolean canHurt(Player attacker, Player victim, boolean viceversa);

    public abstract boolean canHurt(Player attacker, Player victim);

    public abstract boolean canHurt(Player attacker, Entity victim);

    public abstract boolean canUseMyPet(MyPetPlayer player);

    public abstract boolean canMyPetFlyAt(Location location);

    public abstract boolean isInParty(Player player);

    public abstract List<Player> getPartyMembers(Player player);
}
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

package de.Keyle.MyPet.util.hooks;

import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusEntityHook;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusPlayerHook;
import net.sacredlabyrinth.Phaed.PreciousStones.PreciousStones;
import net.sacredlabyrinth.Phaed.PreciousStones.field.FieldFlag;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

@PluginHookName("PreciousStones")
public class PreciousStonesHook implements PlayerVersusPlayerHook, PlayerVersusEntityHook {

    @Override
    public boolean canHurt(Player attacker, Entity defender) {
        try {
            if (defender instanceof Villager) {
                return !PreciousStones.API().flagAppliesToPlayer(attacker, FieldFlag.PROTECT_VILLAGERS, defender.getLocation());
            } else if (defender instanceof Ageable) {
                return !PreciousStones.API().flagAppliesToPlayer(attacker, FieldFlag.PROTECT_ANIMALS, defender.getLocation());
            } else {
                return !PreciousStones.API().flagAppliesToPlayer(attacker, FieldFlag.PROTECT_MOBS, defender.getLocation());
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    @Override
    public boolean canHurt(Player attacker, Player defender) {
        try {
            return !PreciousStones.API().flagAppliesToPlayer(defender, FieldFlag.PREVENT_PVP, defender.getLocation());
        } catch (Throwable ignored) {
        }
        return true;
    }
}
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
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusPlayerHook;
import org.bukkit.entity.Player;
import org.kingdoms.constants.land.Land;
import org.kingdoms.constants.player.KingdomPlayer;
import org.kingdoms.main.Kingdoms;
import org.kingdoms.manager.game.GameManagement;

@PluginHookName("Kingdoms")
public class KingdomsHook implements PlayerVersusPlayerHook {

    @Override
    public boolean canHurt(Player attacker, Player defender) {
        try {
            KingdomPlayer attacked = GameManagement.getPlayerManager().getSession(attacker);
            if (attacked == null) {
                return true;
            }
            if (attacked.isAdminMode()) {
                return true;
            }
            if (attacked.getKingdom() == null) {
                return true;
            }

            KingdomPlayer damaged = GameManagement.getPlayerManager().getSession(defender);
            if (Kingdoms.config.freePvPInWarZone) {
                Land att = GameManagement.getLandManager().getOrLoadLand(damaged.getLoc());
                if (att.getOwner() != null && att.getOwner().equals("WarZone")) {
                    return true;
                }
            }
            return damaged.getKingdom() == null || !attacked.getKingdom().isAllianceWith(damaged.getKingdom());
        } catch (Throwable ignored) {
        }
        return true;
    }
}
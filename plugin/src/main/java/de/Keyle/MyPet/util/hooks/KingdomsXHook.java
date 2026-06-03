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

package de.Keyle.MyPet.util.hooks;

import de.Keyle.MyPet.api.util.service.Load;
import de.Keyle.MyPet.api.util.service.RequiresPlugin;
import de.Keyle.MyPet.api.util.service.ServiceName;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusPlayerHook;
import org.bukkit.entity.Player;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.group.model.relationships.KingdomRelation;
import org.kingdoms.constants.player.KingdomPlayer;

@ServiceName("KingdomsX")
@RequiresPlugin("KingdomsX")
@Load(Load.State.Hooks)
public class KingdomsXHook implements PlayerVersusPlayerHook {

    @Override
    public boolean canHurt(Player attacker, Player defender) {
        try {
            KingdomPlayer attacked = KingdomPlayer.getKingdomPlayer(attacker);
            if (attacked.isAdmin()) {
                return true;
            }
            Kingdom attackerKingdom = attacked.getKingdom();
            if (attackerKingdom == null) {
                return true;
            }

            Kingdom defenderKingdom = KingdomPlayer.getKingdomPlayer(defender).getKingdom();
            return defenderKingdom == null
                    || attackerKingdom.getRelationWith(defenderKingdom) != KingdomRelation.ALLY;
        } catch (Throwable ignored) {
        }
        return true;
    }
}
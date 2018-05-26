/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2018 Keyle
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

import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.hooks.types.AllowedHook;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusPlayerHook;
import me.NoChance.PvPManager.PvPlayer;
import org.bukkit.entity.Player;

@PluginHookName("PvPManager")
public class PvPManagerHook implements PlayerVersusPlayerHook, AllowedHook {

    @Override
    public boolean onEnable() {
        return Configuration.Hooks.PvPManager.DESPAWN_PETS_IN_COMBAT ||
                Configuration.Hooks.PvPManager.RESPECT_PVP_RULES ||
                Configuration.Hooks.PvPManager.PREVENT_DAMAGE_IN_COMBAT;
    }

    @Override
    public boolean canHurt(Player attacker, Player defender) {
        try {
            PvPlayer pvpAttacker = PvPlayer.get(attacker);
            PvPlayer pvpDefender = PvPlayer.get(defender);
            if (Configuration.Hooks.PvPManager.RESPECT_PVP_RULES) {
                if (pvpAttacker.hasOverride()) {
                    return true;
                }
                if (pvpDefender.hasRespawnProtection() || pvpAttacker.hasRespawnProtection()) {
                    return false;
                }
                if (pvpDefender.isNewbie() || pvpAttacker.isNewbie()) {
                    return false;
                }
                if (!pvpDefender.hasPvPEnabled() || !pvpAttacker.hasPvPEnabled()) {
                    return false;
                }
            }
            if (Configuration.Hooks.PvPManager.PREVENT_DAMAGE_IN_COMBAT && pvpDefender.isInCombat()) {
                return false;
            }
            return true;
        } catch (Throwable ignored) {
        }
        return true;
    }

    @Override
    public boolean isPetAllowed(MyPetPlayer owner) {
        if (Configuration.Hooks.PvPManager.DESPAWN_PETS_IN_COMBAT) {
            try {
                Player player = owner.getPlayer();
                PvPlayer pvpPlayer = PvPlayer.get(player);

                return !pvpPlayer.isInCombat();
            } catch (Throwable ignored) {
            }
        }
        return true;
    }
}
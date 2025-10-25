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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusPlayerHook;
import net.brcdev.gangs.GangsPlugin;
import net.brcdev.gangs.GangsPlusApi;
import net.brcdev.gangs.config.Settings;
import net.brcdev.gangs.gang.Gang;
import org.bukkit.entity.Player;

import java.util.Objects;

@PluginHookName("GangsPlus")
public class GangsPlusHook implements PlayerVersusPlayerHook {

    GangsPlugin plugin;

    @Override
    public boolean onEnable() {
        plugin = MyPetApi.getPluginHookManager().getPluginInstance(GangsPlugin.class).get();
        return true;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean canHurt(Player attacker, Player defender) {
        try {
            if (!GangsPlusApi.isInGang(attacker) || !GangsPlusApi.isInGang(defender)) {
                return true;
            }

            Gang attackerGang = GangsPlusApi.getPlayersGang(attacker);
            Gang defenderGang = GangsPlusApi.getPlayersGang(defender);

            if (Objects.equals(attackerGang, defenderGang)) {
                if (Settings.friendlyFire && !Settings.disableFriendlyFireInWorlds.contains(defender.getWorld().getName())) {
                    if (!Settings.friendlyFireTogglableByLeader || attackerGang.isFriendlyFire()) {
                        return true;
                    }
                }
            } else {
                if (!attackerGang.isAlly(defenderGang)) {
                    if (!Settings.enableModuleAlliances || Settings.alliancesAllowPvp) {
                        return true;
                    } else if (plugin.fightManager.areFighting(attackerGang, defenderGang)) {
                        return true;
                    }
                } else if (!Settings.enableModuleAlliances || Settings.alliancesAllowPvp) {
                    return true;
                } else if (plugin.fightManager.areFighting(attackerGang, defenderGang)) {
                    return true;
                }
            }
            return false;
        } catch (Throwable ignored) {
        }
        return true;
    }
}

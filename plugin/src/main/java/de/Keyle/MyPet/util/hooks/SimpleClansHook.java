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
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.ClanPlayer;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import org.bukkit.entity.Player;

@PluginHookName("SimpleClans")
public class SimpleClansHook implements PlayerVersusPlayerHook {

    SimpleClans simpleClans;

    @Override
    public boolean onEnable() {
        simpleClans = MyPetApi.getPluginHookManager().getPluginInstance(SimpleClans.class).get();
        return true;
    }

    @Override
    public boolean canHurt(Player attacker, Player defender) {
        try {
            if (simpleClans.getSettingsManager().isBlacklistedWorld(defender.getLocation().getWorld().getName())) {
                return true;
            }

            ClanPlayer acp = simpleClans.getClanManager().getClanPlayer(attacker);
            ClanPlayer vcp = simpleClans.getClanManager().getClanPlayer(defender);

            Clan vclan = vcp == null ? null : vcp.getClan();
            Clan aclan = acp == null ? null : acp.getClan();
            if (simpleClans.getSettingsManager().isPvpOnlywhileInWar()) {
                if ((aclan == null) || (vclan == null)) {
                    return false;
                }
                if (simpleClans.getPermissionsManager().has(defender, "simpleclans.mod.nopvpinwar")) {
                    return false;
                }
                if (!aclan.isWarring(vclan)) {
                    return false;
                }
            }
            if (vclan != null) {
                if (aclan != null) {
                    if (vcp.isFriendlyFire()) {
                        return true;
                    }
                    if (vclan.isFriendlyFire()) {
                        return true;
                    }
                    if (simpleClans.getSettingsManager().isGlobalff()) {
                        return true;
                    }
                    if (vclan.equals(aclan)) {
                        return false;
                    }
                    if (vclan.isAlly(aclan.getTag())) {
                        return false;
                    }
                } else if (simpleClans.getSettingsManager().getSafeCivilians()) {
                    return false;
                }
            } else if (simpleClans.getSettingsManager().getSafeCivilians()) {
                return false;
            }
        } catch (Throwable ignored) {
        }
        return true;
    }
}
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

import com.ancientshores.Ancient.Guild.AncientGuild;
import com.ancientshores.Ancient.Party.AncientParty;
import de.Keyle.MyPet.api.util.hooks.PluginHookName;
import de.Keyle.MyPet.api.util.hooks.types.PartyHook;
import de.Keyle.MyPet.api.util.hooks.types.PlayerVersusPlayerHook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@PluginHookName("Ancient")
public class AncientHook implements PlayerVersusPlayerHook, PartyHook {

    public boolean canHurt(Player attacker, Player defender) {
        try {
            AncientParty party = AncientParty.getPlayersParty(attacker.getUniqueId());
            if (party != null) {
                if (!party.isFriendlyFireEnabled() && party.containsUUID(defender.getUniqueId())) {
                    return false;
                }
            }

            AncientGuild guild = AncientGuild.getPlayersGuild(attacker.getUniqueId());
            if (guild != null) {
                if (!guild.friendlyFire && guild == AncientGuild.getPlayersGuild(defender.getUniqueId())) {
                    return false;
                }
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    @Override
    public boolean isInParty(Player player) {
        try {
            return AncientParty.getPlayersParty(player.getUniqueId()) != null;
        } catch (Exception ignored) {
        }
        return false;
    }

    @Override
    public List<Player> getPartyMembers(Player player) {
        try {
            AncientParty party = AncientParty.getPlayersParty(player.getUniqueId());
            if (party != null) {
                List<Player> members = new ArrayList<>();
                for (UUID memberUUID : party.getMembers()) {
                    Player member = Bukkit.getPlayer(memberUUID);
                    if (member.isOnline()) {
                        members.add(member);
                    }
                }
                return members;
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
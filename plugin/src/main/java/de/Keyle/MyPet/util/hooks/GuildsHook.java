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
import me.glaremasters.guilds.Guilds;
import me.glaremasters.guilds.guild.Guild;
import me.glaremasters.guilds.utils.ConfigUtils;
import org.bukkit.entity.Player;

import java.util.UUID;

;

@PluginHookName("Guilds")
public class GuildsHook implements PlayerVersusPlayerHook {

    @Override
    public boolean canHurt(Player attacker, Player defender) {
        try {
            Guild playerGuild = getGuild(defender.getUniqueId());
            Guild damagerGuild = getGuild(attacker.getUniqueId());
            if (playerGuild == null || damagerGuild == null) {
                return true;
            }
            if (!ConfigUtils.getBoolean("allow-guild-damage") && playerGuild.equals(damagerGuild)) {
                return false;
            }
            if (!ConfigUtils.getBoolean("allow-ally-damage") && areAllies(playerGuild, damagerGuild)) {
                return false;
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    protected boolean areAllies(Guild guild1, Guild guild2) {
        return guild1.getAllies().contains(guild2.getName());
    }

    protected Guild getGuild(UUID uuid) {
        return Guilds.getGuilds()
                .getGuildHandler()
                .getGuilds()
                .values()
                .stream()
                .filter(guild -> guild.getMembers().stream().anyMatch(member -> member.getUniqueId().equals(uuid)))
                .findFirst()
                .orElse(null);
    }
}
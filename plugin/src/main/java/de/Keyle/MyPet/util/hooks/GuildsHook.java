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
import me.glaremasters.guilds.Guilds;
import me.glaremasters.guilds.configuration.sections.GuildSettings;
import me.glaremasters.guilds.guild.Guild;
import me.glaremasters.guilds.libs.configme.SettingsManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@ServiceName("Guilds")
@RequiresPlugin("Guilds")
@Load(Load.State.Hooks)
public class GuildsHook implements PlayerVersusPlayerHook {

    @Override
    public boolean canHurt(Player attacker, Player defender) {
        try {
            Guild playerGuild = Guilds.getApi().getGuild(defender);
            Guild damagerGuild = Guilds.getApi().getGuild(attacker);
            if (playerGuild == null || damagerGuild == null) {
                return true;
            }
            SettingsManager conf = ((Guilds) Bukkit.getPluginManager().getPlugin("Guilds"))
                    .getSettingsHandler().getMainConf();
            if (!conf.getProperty(GuildSettings.GUILD_DAMAGE) && playerGuild.equals(damagerGuild)) {
                return false;
            }
            if (!conf.getProperty(GuildSettings.ALLY_DAMAGE)
                    && Guilds.getApi().getGuildHandler().isAlly(playerGuild, damagerGuild)) {
                return false;
            }
        } catch (Throwable ignored) {
        }
        return true;
    }
}
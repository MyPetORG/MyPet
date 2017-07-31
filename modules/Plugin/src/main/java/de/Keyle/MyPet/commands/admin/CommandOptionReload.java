/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2017 Keyle
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

package de.Keyle.MyPet.commands.admin;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.commands.CommandOption;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.util.ConfigurationLoader;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;

public class CommandOptionReload implements CommandOption {
    @Override
    public boolean onCommandOption(CommandSender sender, String[] args) {
        MyPetApi.getPlugin().reloadConfig();
        ConfigurationLoader.loadConfiguration();

        if (MyPetApi.getLogger() instanceof MyPetLogger) {
            ((MyPetLogger) MyPetApi.getLogger()).updateDebugLoggerLogLevel();
        }

        Translation.init();

        for (int i = 0; i < Configuration.Misc.MAX_STORED_PET_COUNT; i++) {
            try {
                Bukkit.getPluginManager().addPermission(new Permission("MyPet.petstorage.limit." + i));
            } catch (Exception ignored) {
            }
        }

        sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] config loaded!");

        return true;
    }
}
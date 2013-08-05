/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
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

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.commands.CommandOption;
import de.Keyle.MyPet.util.Configuration;
import de.Keyle.MyPet.util.logger.DebugLogger;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class CommandOptionReload implements CommandOption
{
    @Override
    public boolean onCommandOption(CommandSender sender, String[] args)
    {
        MyPetPlugin.getPlugin().reloadConfig();
        Configuration.config = MyPetPlugin.getPlugin().getConfig();
        Configuration.loadConfiguration();
        DebugLogger.info("Config reloaded.");
        sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] config (config.yml) reloaded!");

        return true;
    }
}
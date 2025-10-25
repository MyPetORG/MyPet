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

package de.Keyle.MyPet.commands.admin;

import de.Keyle.MyPet.api.commands.CommandOption;
import de.Keyle.MyPet.util.Updater;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class CommandOptionUpdate implements CommandOption {
    @Override
    public boolean onCommandOption(CommandSender sender, String[] args) {
        if (Updater.isUpdateAvailable()) {
            sender.sendMessage("A new version is available: " + ChatColor.GOLD + Updater.getLatest());
        } else {
            sender.sendMessage("Your version of MyPet is up to date.");
        }
        return true;
    }
}
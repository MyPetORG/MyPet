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

package de.Keyle.MyPet.commands.mypet;

import de.Keyle.MyPet.api.MyPetVersion;
import de.Keyle.MyPet.api.commands.CommandCategory;
import de.Keyle.MyPet.api.commands.CommandOption;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.util.Updater;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandOptionUpdate implements CommandOption {

    @Override
    public String getHelpTranslationKey() {
        return "Message.Command.Help.Update";
    }

    @Override
    public String getHelpCommand() {
        return "/mypet update";
    }

    @Override
    public CommandCategory getHelpCategory() {
        return CommandCategory.ADMIN;
    }

    @Override
    public boolean isVisibleTo(Player player) {
        return Permissions.has(player, "MyPet.admin", false);
    }

    @Override
    public int getHelpOrder() {
        return 14;
    }

    @Override
    public boolean onCommandOption(CommandSender sender, String[] args) {
        if (Updater.isUpdateAvailable()) {
            sender.sendMessage("A new version is available: " + ChatColor.GOLD + Updater.getLatest());
        } else if ("local".equals(MyPetVersion.getBuild())) {
            sender.sendMessage("You are running a " + ChatColor.YELLOW + "local build" + ChatColor.RESET + ". Update checks are skipped.");
        } else {
            sender.sendMessage("Your version of MyPet is up to date.");
        }
        return true;
    }
}
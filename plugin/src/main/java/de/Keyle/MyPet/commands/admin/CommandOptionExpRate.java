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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration.LevelSystem.Experience.Modifier;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.commands.CommandOptionTabCompleter;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandOptionExpRate implements CommandOptionTabCompleter {

    private static List<String> addSetRemoveList = new ArrayList<>();

    static {
        addSetRemoveList.add("global");
    }

    @Override
    public boolean onCommandOption(CommandSender sender, String[] args) {
        String lang = MyPetApi.getPlatformHelper().getCommandSenderLanguage(sender);

        if (args.length == 0 || !args[0].toLowerCase().equals("global")) {
            sender.sendMessage(Translation.getString("Message.Command.Help.MissingParameter", lang));
            sender.sendMessage(" -> " + ChatColor.DARK_AQUA + "/petadmin exp-rate " + ChatColor.RED + "global");
            return false;
        }

        if (args.length == 1) {
            sender.sendMessage("Global Exp Rate: " + ChatColor.DARK_AQUA + Modifier.GLOBAL);
        } else {
            if (!Util.isDouble(args[1])) {
                sender.sendMessage(Translation.getString("Message.Command.Help.MissingParameter", lang));
                sender.sendMessage(" -> " + ChatColor.DARK_AQUA + "/petadmin exp-rate " + args[0].toLowerCase() + " " + ChatColor.RED + "<amount>");
                return false;
            }

            switch (args[0]) {
                case "global":
                    Modifier.GLOBAL = Double.parseDouble(args[1]);
                    break;
            }
        }


        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, String[] strings) {
        if (strings.length == 2) {
            return filterTabCompletionResults(addSetRemoveList, strings[1]);
        }
        return Collections.emptyList();
    }
}
/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2016 Keyle
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

package de.Keyle.MyPet.commands;

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.repository.MyPetList;
import de.Keyle.MyPet.util.Colorizer;
import de.Keyle.MyPet.util.Configuration;
import de.Keyle.MyPet.util.Util;
import de.Keyle.MyPet.util.hooks.Permissions;
import de.Keyle.MyPet.util.locale.Translation;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandName implements CommandExecutor, TabCompleter {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player petOwner = (Player) sender;
            if (MyPetList.hasActiveMyPet(petOwner)) {
                if (args.length < 1) {
                    return false;
                }

                MyPet myPet = MyPetList.getMyPet(petOwner);
                if (!Permissions.has(petOwner, "MyPet.user.command.name")) {
                    myPet.sendMessageToOwner(Translation.getString("Message.No.CanUse", petOwner));
                    return true;
                }

                String name = "";
                for (String arg : args) {
                    if (!name.equals("")) {
                        name += " ";
                    }
                    name += arg;
                }
                name = Colorizer.setColors(name);

                Pattern regex = Pattern.compile("§[abcdefklmnor0-9]");
                Matcher regexMatcher = regex.matcher(name);
                if (regexMatcher.find()) {
                    name += ChatColor.RESET;
                }

                String nameWihtoutColors = Util.cutString(ChatColor.stripColor(name), 64);
                name = Util.cutString(name, 64);

                if (nameWihtoutColors.length() <= Configuration.MAX_PET_NAME_LENGTH) {
                    if (Permissions.has(petOwner, "MyPet.user.command.name.color")) {
                        myPet.setPetName(name);
                        sender.sendMessage(Util.formatText(Translation.getString("Message.Command.Name.New", petOwner), name));
                    } else {
                        myPet.setPetName(nameWihtoutColors);
                        sender.sendMessage(Util.formatText(Translation.getString("Message.Command.Name.New", petOwner), nameWihtoutColors));
                    }
                } else {
                    sender.sendMessage(Util.formatText(Translation.getString("Message.Command.Name.ToLong", petOwner), name, Configuration.MAX_PET_NAME_LENGTH));
                }
            } else {
                sender.sendMessage(Translation.getString("Message.No.HasPet", petOwner));
            }
            return true;
        }
        sender.sendMessage("You can't use this command from server console!");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] strings) {
        return CommandAdmin.EMPTY_LIST;
    }
}
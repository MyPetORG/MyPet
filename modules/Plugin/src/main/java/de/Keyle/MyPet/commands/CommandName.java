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

package de.Keyle.MyPet.commands;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.commands.CommandTabCompleter;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.Colorizer;
import de.Keyle.MyPet.api.util.NameFilter;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandName implements CommandTabCompleter {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player petOwner = (Player) sender;
            if (WorldGroup.getGroupByWorld(petOwner.getWorld()).isDisabled()) {
                petOwner.sendMessage(Translation.getString("Message.No.AllowedHere", petOwner));
                return true;
            }
            if (MyPetApi.getMyPetManager().hasActiveMyPet(petOwner)) {
                if (args.length < 1) {
                    return false;
                }

                MyPet myPet = MyPetApi.getMyPetManager().getMyPet(petOwner);
                if (!Permissions.has(petOwner, "MyPet.command.name")) {
                    myPet.getOwner().sendMessage(Translation.getString("Message.No.CanUse", petOwner));
                    return true;
                }

                String name = "";
                for (String arg : args) {
                    if (!name.equals("")) {
                        name += " ";
                    }
                    name += arg;
                }

                if (!NameFilter.isClean(name)) {
                    sender.sendMessage(Translation.getString("Message.Command.Name.Filter", petOwner));
                    return true;
                }

                name = Colorizer.setColors(name);

                Pattern regex = Pattern.compile("§[abcdefklmnor0-9]");
                Matcher regexMatcher = regex.matcher(name);
                if (regexMatcher.find()) {
                    name += ChatColor.RESET;
                }

                String nameWihtoutColors = Util.cutString(ChatColor.stripColor(name), 64);
                name = Util.cutString(name, 64);

                if (nameWihtoutColors.length() <= Configuration.Name.MAX_LENGTH) {
                    myPet.setPetName(name);
                    if (Permissions.has(petOwner, "MyPet.command.name.color")) {
                        sender.sendMessage(Util.formatText(Translation.getString("Message.Command.Name.New", petOwner), name));
                    } else {
                        sender.sendMessage(Util.formatText(Translation.getString("Message.Command.Name.New", petOwner), nameWihtoutColors));
                    }
                } else {
                    sender.sendMessage(Util.formatText(Translation.getString("Message.Command.Name.ToLong", petOwner), name, Configuration.Name.MAX_LENGTH));
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
        return Collections.emptyList();
    }
}
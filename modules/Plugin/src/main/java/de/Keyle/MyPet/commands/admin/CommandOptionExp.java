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
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.commands.CommandOptionTabCompleter;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandOptionExp implements CommandOptionTabCompleter {

    private static List<String> addSetRemoveList = new ArrayList<>();

    static {
        addSetRemoveList.add("add");
        addSetRemoveList.add("set");
        addSetRemoveList.add("remove");
    }

    @Override
    public boolean onCommandOption(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Translation.getString("Message.Command.Help.MissingParameter", sender));
            sender.sendMessage(" -> " + ChatColor.DARK_AQUA + "/petadmin exp " + ChatColor.RED + "<a player name>");
            return false;
        }
        if (args.length < 2) {
            sender.sendMessage(Translation.getString("Message.Command.Help.MissingParameter", sender));
            sender.sendMessage(" -> " + ChatColor.DARK_AQUA + "/petadmin exp " + args[0] + " " + ChatColor.RED + "<amount>");
            return false;
        }

        String lang = MyPetApi.getPlatformHelper().getCommandSenderLanguage(sender);
        Player petOwner = Bukkit.getServer().getPlayer(args[0]);

        if (petOwner == null || !petOwner.isOnline()) {
            sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] " + Translation.getString("Message.No.PlayerOnline", lang));
            return true;
        } else if (!MyPetApi.getMyPetManager().hasActiveMyPet(petOwner)) {
            sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] " + Util.formatText(Translation.getString("Message.No.UserHavePet", lang), petOwner.getName()));
            return true;
        }
        MyPet myPet = MyPetApi.getMyPetManager().getMyPet(petOwner);

        String value = args[1];
        boolean level = false;
        double exp = myPet.getExp();

        if (value.endsWith("l") || value.endsWith("L")) {
            level = true;
            value = value.substring(0, value.length() - 1);
        }

        if (args.length == 2 || args[2].equalsIgnoreCase("set")) {
            if (level) {
                if (Util.isInt(value)) {
                    exp = myPet.getExperience().getExpByLevel(Math.min(Integer.parseInt(value), Configuration.LevelSystem.Experience.LEVEL_CAP));
                }
            } else {
                if (Util.isDouble(value)) {
                    exp = Math.min(Double.parseDouble(value), myPet.getExperience().getMaxExp());
                }
            }
        } else if (args[2].equalsIgnoreCase("add")) {
            if (level) {
                if (Util.isInt(value)) {
                    int newLevel = Math.min(myPet.getExperience().getLevel() + Integer.parseInt(value), Configuration.LevelSystem.Experience.LEVEL_CAP);

                    exp = myPet.getExperience().getExpByLevel(newLevel);
                }
            } else {
                if (Util.isDouble(value)) {
                    exp = Math.min(Double.parseDouble(value) + exp, myPet.getExperience().getMaxExp());
                }
            }
        } else if (args[2].equalsIgnoreCase("remove")) {
            if (level) {
                if (Util.isInt(value)) {
                    int oldLevel = myPet.getExperience().getLevel();
                    if (oldLevel - Integer.parseInt(value) <= 1) {
                        exp = 0;
                    } else {
                        exp = myPet.getExperience().getExpByLevel(oldLevel - Integer.parseInt(value));
                    }
                }
            } else {
                if (Util.isDouble(value)) {
                    exp -= Double.parseDouble(value);
                }
            }
        }

        myPet.getExperience().setExp(exp);
        sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] set exp to " + exp + ". Pet is now level " + myPet.getExperience().getLevel() + ".");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, String[] strings) {
        if (strings.length == 2) {
            return null;
        } else if (strings.length == 4) {
            return filterTabCompletionResults(addSetRemoveList, strings[3]);
        }
        return Collections.emptyList();
    }
}
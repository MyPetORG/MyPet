/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

import de.Keyle.MyPet.api.commands.CommandOptionTabCompleter;
import de.Keyle.MyPet.commands.CommandAdmin;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.repository.MyPetList;
import de.Keyle.MyPet.util.BukkitUtil;
import de.Keyle.MyPet.util.Util;
import de.Keyle.MyPet.util.locale.Locales;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandOptionRespawn implements CommandOptionTabCompleter {
    private static List<String> showList = new ArrayList<String>();

    static {
        showList.add("show");
        showList.add("<number>");
    }

    @Override
    public boolean onCommandOption(CommandSender sender, String[] args) {
        if (args.length < 1) {
            return false;
        }

        String lang = BukkitUtil.getCommandSenderLanguage(sender);
        Player petOwner = Bukkit.getServer().getPlayer(args[0]);

        if (petOwner == null || !petOwner.isOnline()) {
            sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] " + Locales.getString("Message.No.PlayerOnline", lang));
            return true;
        } else if (!MyPetList.hasMyPet(petOwner)) {
            sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] " + Util.formatText(Locales.getString("Message.No.UserHavePet", lang), petOwner.getName()));
            return true;
        }
        MyPet myPet = MyPetList.getMyPet(petOwner);
        if (args.length >= 2 && args[1].equalsIgnoreCase("show")) {
            sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] respawn time: " + myPet.getRespawnTime() + "sec.");
        } else if (myPet.getStatus() == PetState.Dead) {
            if (args.length >= 2 && Util.isInt(args[1])) {
                int respawnTime = Integer.parseInt(args[1]);
                if (respawnTime >= 0) {
                    myPet.setRespawnTime(respawnTime);
                }
            } else {
                myPet.setRespawnTime(0);
            }
            sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] set respawn time to: " + myPet.getRespawnTime() + "sec.");
        } else {
            sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] pet is not dead!");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, String[] strings) {
        if (strings.length == 2) {
            return null;
        }
        if (strings.length == 3) {
            return showList;
        }
        return CommandAdmin.emptyList;
    }
}
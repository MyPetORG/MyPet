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
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.commands.CommandTabCompleter;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.skilltree.Skill;
import de.Keyle.MyPet.api.util.Colorizer;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandSkill implements CommandTabCompleter {

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player petOwner;
        if (args.length == 0 && sender instanceof Player) {
            petOwner = (Player) sender;
        } else if (args.length > 0 && (!(sender instanceof Player) || Permissions.has((Player) sender, "MyPet.admin"))) {
            petOwner = Bukkit.getServer().getPlayer(args[0]);

            if (petOwner == null || !petOwner.isOnline()) {
                sender.sendMessage(Translation.getString("Message.No.PlayerOnline", sender));
                return true;
            } else if (!MyPetApi.getMyPetManager().hasActiveMyPet(petOwner)) {
                sender.sendMessage(Util.formatText(Translation.getString("Message.No.UserHavePet", sender), petOwner.getName()));
                return true;
            }
        } else {
            if (sender instanceof Player) {
                sender.sendMessage(Translation.getString("Message.No.AllowedHere", sender));
            } else {
                sender.sendMessage("You can't use this command from server console!");
            }
            return true;
        }

        if (WorldGroup.getGroupByWorld(petOwner.getWorld()).isDisabled()) {
            sender.sendMessage(Translation.getString("Message.No.AllowedHere", sender));
        }

        if (MyPetApi.getMyPetManager().hasActiveMyPet(petOwner)) {
            MyPet myPet = MyPetApi.getMyPetManager().getMyPet(petOwner);
            myPet.autoAssignSkilltree();
            sender.sendMessage(Util.formatText(Translation.getString("Message.Command.Skills.Show", sender), myPet.getPetName(), myPet.getSkilltree() == null ? "-" : Colorizer.setColors(myPet.getSkilltree().getDisplayName())));

            for (Skill skill : myPet.getSkills().all()) {
                if (skill.isActive()) {
                    sender.sendMessage("  " + ChatColor.GREEN + skill.getName(MyPetApi.getPlatformHelper().getCommandSenderLanguage(sender)) + ChatColor.RESET + " " + skill.toPrettyString(MyPetApi.getPlatformHelper().getCommandSenderLanguage(sender)));
                }
            }
            return true;
        } else {
            sender.sendMessage(Translation.getString("Message.No.HasPet", sender));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] strings) {
        if (strings.length == 1) {
            if (sender instanceof Player) {
                if (Permissions.has((Player) sender, "MyPet.command.info.other")) {
                    return null;
                }
            } else {
                return null;
            }
        }
        return Collections.emptyList();
    }
}
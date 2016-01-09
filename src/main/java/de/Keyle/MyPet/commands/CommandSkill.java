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
import de.Keyle.MyPet.skill.skills.implementation.ISkillInstance;
import de.Keyle.MyPet.util.BukkitUtil;
import de.Keyle.MyPet.util.Util;
import de.Keyle.MyPet.util.hooks.Permissions;
import de.Keyle.MyPet.util.locale.Translation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandSkill implements CommandExecutor, TabCompleter {
    private static List<String> emptyList = new ArrayList<>();

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player petOwner = (Player) sender;
            if (args.length > 0 && Permissions.has(petOwner, "MyPet.admin", false)) {
                petOwner = Bukkit.getServer().getPlayer(args[0]);

                if (petOwner == null || !petOwner.isOnline()) {
                    sender.sendMessage(Translation.getString("Message.No.PlayerOnline", petOwner));
                    return true;
                } else if (!MyPetList.hasActiveMyPet(petOwner)) {
                    sender.sendMessage(Util.formatText(Translation.getString("Message.No.UserHavePet", petOwner), petOwner.getName()));
                    return true;
                }
            }

            if (MyPetList.hasActiveMyPet(petOwner)) {
                MyPet myPet = MyPetList.getMyPet(petOwner);
                myPet.autoAssignSkilltree();
                sender.sendMessage(Util.formatText(Translation.getString("Message.Command.Skills.Show", petOwner), myPet.getPetName(), (myPet.getSkillTree() == null ? "-" : myPet.getSkillTree().getDisplayName())));

                for (ISkillInstance skill : myPet.getSkills().getSkills()) {
                    if (skill.isActive()) {
                        sender.sendMessage("  " + ChatColor.GREEN + skill.getName(BukkitUtil.getPlayerLanguage(petOwner)) + ChatColor.RESET + " " + skill.getFormattedValue());
                    }
                }
                return true;
            } else {
                sender.sendMessage(Translation.getString("Message.No.HasPet", petOwner));
            }
            return true;
        }
        sender.sendMessage("You can't use this command from server console!");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if (strings.length == 1 && Permissions.has((Player) commandSender, "MyPet.admin", false)) {
            return null;
        }
        return emptyList;
    }
}
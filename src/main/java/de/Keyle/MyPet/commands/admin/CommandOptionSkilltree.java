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

package de.Keyle.MyPet.commands.admin;

import de.Keyle.MyPet.api.commands.CommandOptionTabCompleter;
import de.Keyle.MyPet.commands.CommandAdmin;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.repository.MyPetList;
import de.Keyle.MyPet.skill.skilltree.SkillTree;
import de.Keyle.MyPet.skill.skilltree.SkillTreeMobType;
import de.Keyle.MyPet.util.BukkitUtil;
import de.Keyle.MyPet.util.Util;
import de.Keyle.MyPet.util.locale.Translation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandOptionSkilltree implements CommandOptionTabCompleter {
    @Override
    public boolean onCommandOption(CommandSender sender, String[] args) {
        if (args.length < 2) {
            return false;
        }

        String lang = BukkitUtil.getCommandSenderLanguage(sender);
        Player petOwner = Bukkit.getServer().getPlayer(args[0]);

        if (petOwner == null || !petOwner.isOnline()) {
            sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] " + Translation.getString("Message.No.PlayerOnline", lang));
            return true;
        } else if (!MyPetList.hasActiveMyPet(petOwner)) {
            sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] " + Util.formatText(Translation.getString("Message.No.UserHavePet", lang), petOwner.getName()));
            return true;
        }
        MyPet myPet = MyPetList.getMyPet(petOwner);

        SkillTreeMobType skillTreeMobType = SkillTreeMobType.getMobTypeByName(myPet.getPetType().getTypeName());
        if (skillTreeMobType.hasSkillTree(args[1])) {
            SkillTree skillTree = skillTreeMobType.getSkillTree(args[1]);
            if (myPet.setSkilltree(skillTree)) {
                sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] " + Util.formatText(Translation.getString("Message.Skilltree.SwitchedToFor", lang), petOwner.getName(), skillTree.getName()));
            } else {
                sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] " + Translation.getString("Message.Skilltree.NotSwitched", lang));
            }
        } else {
            sender.sendMessage("[" + ChatColor.AQUA + "MyPet" + ChatColor.RESET + "] " + Util.formatText(Translation.getString("Message.Command.Skilltree.CantFindSkilltree", lang), args[1]));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, String[] strings) {
        if (strings.length == 2) {
            return null;
        }
        if (strings.length == 3) {
            Player player = Bukkit.getServer().getPlayer(strings[1]);
            if (player == null || !player.isOnline()) {
                return CommandAdmin.EMPTY_LIST;
            }
            if (MyPetList.hasActiveMyPet(player)) {
                MyPet myPet = MyPetList.getMyPet(player);
                SkillTreeMobType skillTreeMobType = SkillTreeMobType.getMobTypeByName(myPet.getPetType().getTypeName());

                List<String> skilltreeList = new ArrayList<>();
                for (SkillTree skillTree : skillTreeMobType.getSkillTrees()) {
                    skilltreeList.add(skillTree.getName());
                }
                return skilltreeList;
            }
        }
        return CommandAdmin.EMPTY_LIST;
    }
}
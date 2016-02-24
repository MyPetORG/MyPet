/*
 * This file is part of mypet
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet is licensed under the GNU Lesser General Public License.
 *
 * mypet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.commands;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.skill.SkillInfo;
import de.Keyle.MyPet.api.skill.skilltree.SkillTree;
import de.Keyle.MyPet.api.skill.skilltree.SkillTreeLevel;
import de.Keyle.MyPet.api.skill.skilltree.SkillTreeMobType;
import de.Keyle.MyPet.api.util.Colorizer;
import org.bukkit.ChatColor;
import org.bukkit.command.*;

import java.util.List;

public class CommandShowSkillTree implements CommandExecutor, TabCompleter {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof ConsoleCommandSender) {
            SkillTreeMobType mobType;
            if (args.length >= 1) {
                if (args[0].equalsIgnoreCase("default")) {
                    mobType = SkillTreeMobType.DEFAULT;
                } else {
                    MyPetType type = MyPetType.byName(args[0]);
                    if (type != null) {
                        mobType = SkillTreeMobType.byPetType(type);
                    } else {
                        MyPetApi.getLogger().info("There is " + ChatColor.RED + "no" + ChatColor.RESET + " mobtype with the name: " + ChatColor.AQUA + args[0]);
                        return true;
                    }
                }

                if (args.length >= 2) {
                    if (mobType.hasSkillTree(args[1])) {
                        SkillTree skillTree = mobType.getSkillTree(args[1]);
                        MyPetApi.getLogger().info("----- MyPet Skilltree: " + ChatColor.AQUA + skillTree.getName() + ChatColor.RESET + " - Inherits: " + (skillTree.getInheritance() != null ? ChatColor.AQUA + skillTree.getInheritance() + ChatColor.RESET : ChatColor.DARK_GRAY + "none" + ChatColor.RESET) + " -----");
                        for (SkillTreeLevel lvl : skillTree.getLevelList()) {
                            MyPetApi.getLogger().info(ChatColor.YELLOW + " " + lvl.getLevel() + ChatColor.RESET + ": (" + (lvl.hasLevelupMessage() ? Colorizer.setColors(lvl.getLevelupMessage()) + ChatColor.RESET : "-") + ")");
                            for (SkillInfo skill : lvl.getSkills()) {
                                if (skill.isAddedByInheritance()) {
                                    MyPetApi.getLogger().info("   " + ChatColor.DARK_GRAY + skill.getName());
                                } else {
                                    MyPetApi.getLogger().info("   " + skill.getName());
                                }
                            }
                        }
                        MyPetApi.getLogger().info("----- MyPet Skilltree " + ChatColor.AQUA + skillTree.getName() + ChatColor.RESET + " end -----");
                    } else {
                        MyPetApi.getLogger().info("There is " + ChatColor.RED + "no" + ChatColor.RESET + " skilltree with the name: " + ChatColor.AQUA + args[1]);
                    }
                } else {
                    MyPetApi.getLogger().info("----- MyPet Skilltrees for: " + ChatColor.GREEN + args[0]);
                    for (String skillTreeName : mobType.getSkillTreeNames()) {
                        MyPetApi.getLogger().info("   " + skillTreeName);
                    }
                }
            } else {
                return false;
            }
        } else {
            sender.sendMessage("Can only be used in server console");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] strings) {
        return CommandAdmin.EMPTY_LIST;
    }
}
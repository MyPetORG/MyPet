/*
 * Copyright (C) 2011-2013 Keyle
 *
 * This file is part of MyPet
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
 * along with MyPet. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.chatcommands;

import de.Keyle.MyPet.skill.MyPetSkillTree;
import de.Keyle.MyPet.skill.MyPetSkillTreeLevel;
import de.Keyle.MyPet.skill.MyPetSkillTreeMobType;
import de.Keyle.MyPet.skill.MyPetSkillTreeSkill;
import de.Keyle.MyPet.util.MyPetUtil;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

public class CommandShowSkillTree implements CommandExecutor
{
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof ConsoleCommandSender)
        {
            if (args.length == 1)
            {
                if (MyPetSkillTreeMobType.hasMobType(args[0]))
                {
                    MyPetLogger.write("----- MyPet Skilltrees for: " + ChatColor.GREEN + args[0]);
                    for (String skillTreeName : MyPetSkillTreeMobType.getMobTypeByName(args[0]).getSkillTreeNames())
                    {
                        MyPetLogger.write("   " + skillTreeName);
                    }
                    MyPetUtil.getDebugLogger().info("----- MyPet Skilltrees for " + args[0] + " end -----");
                }
                else
                {
                    MyPetLogger.write("There is " + ChatColor.RED + "no" + ChatColor.RESET + " mobtype with the name: " + ChatColor.AQUA + args[0]);
                }
            }
            else if (args.length == 2)
            {
                if (MyPetSkillTreeMobType.hasMobType(args[0]))
                {
                    if (MyPetSkillTreeMobType.getMobTypeByName(args[0]).hasSkillTree(args[1]))
                    {
                        MyPetSkillTree skillTree = MyPetSkillTreeMobType.getMobTypeByName(args[0]).getSkillTree(args[1]);
                        MyPetLogger.write("----- MyPet Skilltree: " + ChatColor.AQUA + skillTree.getName() + ChatColor.RESET + " - Inherits: " + (skillTree.getInheritance() != null ? ChatColor.AQUA + skillTree.getInheritance() + ChatColor.RESET : "none") + " -----");
                        MyPetUtil.getDebugLogger().info("----- Console: MyPet Skilltree: " + skillTree.getName() + " - Inherits: " + skillTree.getInheritance() + " -----");
                        for (MyPetSkillTreeLevel lvl : skillTree.getLevelList())
                        {
                            MyPetLogger.write(ChatColor.YELLOW + " " + lvl.getLevel() + ChatColor.RESET + ":");
                            for (MyPetSkillTreeSkill skill : lvl.getSkills())
                            {
                                MyPetLogger.write("   " + skill.getName());
                                MyPetUtil.getDebugLogger().info("   " + skill.getName());
                            }
                        }
                        MyPetLogger.write("----- MyPet Skilltree " + ChatColor.AQUA + skillTree.getName() + ChatColor.RESET + " end -----");
                        MyPetUtil.getDebugLogger().info("----- MyPet Skilltree end -----");
                    }
                    else
                    {
                        MyPetLogger.write("There is " + ChatColor.RED + "no" + ChatColor.RESET + " skilltree with the name: " + ChatColor.AQUA + args[1]);
                    }
                }
                else
                {
                    MyPetLogger.write("There is " + ChatColor.RED + "no" + ChatColor.RESET + " mobtype with the name: " + ChatColor.AQUA + args[0]);
                }
            }
        }
        else
        {
            sender.sendMessage("Can only be used in server console");
        }
        return true;
    }
}
/*
 * Copyright (C) 2011-2012 Keyle
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
import de.Keyle.MyPet.skill.MyPetSkillTreeSkill;
import de.Keyle.MyPet.util.MyPetSkillTreeConfigLoader;
import de.Keyle.MyPet.util.MyPetUtil;
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
            if (args.length == 1 || args.length == 2)
            {
                if (MyPetSkillTreeConfigLoader.hasMobType(args[0]))
                {
                    if (MyPetSkillTreeConfigLoader.containsSkillTree(args[0], args[1]))
                    {
                        MyPetSkillTree skillTree = MyPetSkillTreeConfigLoader.getMobType(args[0]).getSkillTree(args[1]);
                        MyPetUtil.getLogger().info("----- MyPet Skilltree: " + skillTree.getName() + " -----");
                        MyPetUtil.getDebugLogger().info("----- Console: MyPet Skilltree: " + skillTree.getName() + " -----");
                        for (MyPetSkillTreeLevel lvl : skillTree.getLevelList())
                        {
                            MyPetUtil.getLogger().info(" " + lvl.getLevel() + ":");
                            for (MyPetSkillTreeSkill skill : lvl.getSkills())
                            {
                                MyPetUtil.getLogger().info("   " + skill.getName());
                                MyPetUtil.getDebugLogger().info("   " + skill.getName());
                            }
                        }
                        MyPetUtil.getLogger().info("----- MyPet Skilltree end -----");
                        MyPetUtil.getDebugLogger().info("----- MyPet Skilltree end -----");
                    }
                    else
                    {
                        MyPetUtil.getLogger().info("There is no skilltree with the name: " + args[1]);
                    }
                }
                else
                {
                    MyPetUtil.getLogger().info("There is no mobtype with the name: " + args[0]);
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
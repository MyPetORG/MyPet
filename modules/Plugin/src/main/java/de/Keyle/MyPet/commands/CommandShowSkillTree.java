/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2018 Keyle
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
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import org.bukkit.ChatColor;
import org.bukkit.command.*;

import java.util.Collections;
import java.util.List;

public class CommandShowSkillTree implements CommandExecutor, TabCompleter {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof ConsoleCommandSender) {
            if (args.length >= 1) {
                if (MyPetApi.getSkilltreeManager().hasSkilltree(args[1])) {
                    Skilltree skilltree = MyPetApi.getSkilltreeManager().getSkilltree(args[1]);
                    MyPetApi.getLogger().info("----- MyPet Skilltree: " + ChatColor.AQUA + skilltree.getName() + ChatColor.RESET + " -----");
                    /*
                    for (LevelRule lvl : skilltree.get()) {
                        MyPetApi.getLogger().info(ChatColor.YELLOW + " " + lvl.getLevel() + ChatColor.RESET + ": (" + (lvl.hasLevelupMessage() ? Colorizer.setColors(lvl.getLevelupMessage()) + ChatColor.RESET : "-") + ")");
                        for (SkillInfo skill : lvl.all()) {
                            if (skill.isAddedByInheritance()) {
                                MyPetApi.getLogger().info("   " + ChatColor.DARK_GRAY + skill.getName());
                            } else {
                                MyPetApi.getLogger().info("   " + skill.getName());
                            }
                        }
                    }
                    */
                    MyPetApi.getLogger().info("----- MyPet Skilltree " + ChatColor.AQUA + skilltree.getName() + ChatColor.RESET + " end -----");
                } else {
                    MyPetApi.getLogger().info("There is " + ChatColor.RED + "no" + ChatColor.RESET + " skilltree with the name: " + ChatColor.AQUA + args[1]);
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
        return Collections.emptyList();
    }
}
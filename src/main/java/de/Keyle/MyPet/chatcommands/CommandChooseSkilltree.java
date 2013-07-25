/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
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

package de.Keyle.MyPet.chatcommands;

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetList;
import de.Keyle.MyPet.skill.SkillTree;
import de.Keyle.MyPet.skill.SkillTreeMobType;
import de.Keyle.MyPet.util.Configuration;
import de.Keyle.MyPet.util.Permissions;
import de.Keyle.MyPet.util.Util;
import de.Keyle.MyPet.util.locale.Locales;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandChooseSkilltree implements CommandExecutor, TabCompleter
{
    private static List<String> emptyList = new ArrayList<String>();

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!(sender instanceof Player))
        {
            sender.sendMessage("You can't use this command from server console!");
            return true;
        }
        Player player = (Player) sender;
        if (MyPetList.hasMyPet(player))
        {
            MyPet myPet = MyPetList.getMyPet(player);
            if (Configuration.AUTOMATIC_SKILLTREE_ASSIGNMENT && !myPet.getOwner().isMyPetAdmin())
            {
                myPet.autoAssignSkilltree();
                sender.sendMessage(Locales.getString("Message.AutomaticSkilltreeAssignment", myPet.getOwner().getLanguage()));
            }
            else if (myPet.getSkillTree() != null && Configuration.CHOOSE_SKILLTREE_ONLY_ONCE && !myPet.getOwner().isMyPetAdmin())
            {
                sender.sendMessage(Util.formatText(Locales.getString("Message.OnlyChooseSkilltreeOnce", myPet.getOwner().getLanguage()), myPet.getPetName()));
            }
            else if (SkillTreeMobType.hasMobType(myPet.getPetType().getTypeName()))
            {
                SkillTreeMobType skillTreeMobType = SkillTreeMobType.getMobTypeByName(myPet.getPetType().getTypeName());
                if (args.length >= 1)
                {
                    String skilltreeName = "";
                    for (String arg : args)
                    {
                        skilltreeName += arg + " ";
                    }
                    skilltreeName = skilltreeName.substring(0, skilltreeName.length() - 1);
                    if (skillTreeMobType.hasSkillTree(skilltreeName))
                    {
                        SkillTree skillTree = skillTreeMobType.getSkillTree(skilltreeName);
                        if (Permissions.has(player, "MyPet.custom.skilltree." + skillTree.getPermission()))
                        {
                            if (myPet.setSkilltree(skillTree))
                            {
                                sender.sendMessage(Util.formatText(Locales.getString("Message.SkilltreeSwitchedTo", player), skillTree.getName()));
                                if (myPet.getOwner().isMyPetAdmin() && Configuration.SKILLTREE_SWITCH_PENALTY_ADMIN)
                                {
                                    myPet.getExperience().removeExp(Configuration.SKILLTREE_SWITCH_PENALTY_FIXED);
                                    myPet.getExperience().removeExp(myPet.getExperience().getExp() * Configuration.SKILLTREE_SWITCH_PENALTY_PERCENT / 100.);
                                }
                                else
                                {
                                    myPet.getExperience().removeExp(Configuration.SKILLTREE_SWITCH_PENALTY_FIXED);
                                    myPet.getExperience().removeExp(myPet.getExperience().getExp() * Configuration.SKILLTREE_SWITCH_PENALTY_PERCENT / 100.);
                                }
                            }
                            else
                            {
                                sender.sendMessage(Locales.getString("Message.SkilltreeNotSwitched", player));
                            }
                        }
                        else
                        {
                            sender.sendMessage(Util.formatText(Locales.getString("Message.CantFindSkilltree", player), skilltreeName));
                        }
                    }
                    else
                    {
                        sender.sendMessage(Util.formatText(Locales.getString("Message.CantFindSkilltree", player), skilltreeName));
                    }
                }
                else
                {
                    sender.sendMessage(Util.formatText(Locales.getString("Message.AvailableSkilltrees", player), myPet.getPetName()));
                    for (SkillTree skillTree : skillTreeMobType.getSkillTrees())
                    {
                        if (Permissions.has(player, "MyPet.custom.skilltree." + skillTree.getPermission()))
                        {
                            sender.sendMessage("   " + skillTree.getName());
                        }
                    }
                }
            }
        }
        else
        {
            sender.sendMessage(Locales.getString("Message.DontHavePet", player));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings)
    {
        Player player = (Player) commandSender;
        if (MyPetList.hasMyPet(player))
        {
            MyPet myPet = MyPetList.getMyPet(player);
            if (Configuration.AUTOMATIC_SKILLTREE_ASSIGNMENT && !myPet.getOwner().isMyPetAdmin())
            {
                return emptyList;
            }
            else if (myPet.getSkillTree() != null && Configuration.CHOOSE_SKILLTREE_ONLY_ONCE && !myPet.getOwner().isMyPetAdmin())
            {
                return emptyList;
            }
            else if (SkillTreeMobType.hasMobType(myPet.getPetType().getTypeName()))
            {
                SkillTreeMobType skillTreeMobType = SkillTreeMobType.getMobTypeByName(myPet.getPetType().getTypeName());

                List<String> skilltreeList = new ArrayList<String>();
                for (SkillTree skillTree : skillTreeMobType.getSkillTrees())
                {
                    if (Permissions.has(player, "MyPet.custom.skilltree." + skillTree.getPermission()))
                    {
                        skilltreeList.add(skillTree.getName());
                    }
                }
                return skilltreeList;
            }
        }
        return emptyList;
    }
}
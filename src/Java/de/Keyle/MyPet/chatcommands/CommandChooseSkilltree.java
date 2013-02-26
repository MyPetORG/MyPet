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
import de.Keyle.MyPet.skill.MyPetSkillTree;
import de.Keyle.MyPet.skill.MyPetSkillTreeMobType;
import de.Keyle.MyPet.util.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandChooseSkilltree implements CommandExecutor
{
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!(sender instanceof Player))
        {
            return false;
        }
        Player player = (Player) sender;
        if (MyPetList.hasMyPet(player))
        {
            MyPet myPet = MyPetList.getMyPet(player);
            if (MyPetConfiguration.AUTOMATIC_SKILLTREE_ASSIGNMENT && !myPet.getOwner().isMyPetAdmin())
            {
                sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_AutomaticSkilltreeAssignment")));
            }
            else if (myPet.getSkillTree() != null && MyPetConfiguration.CHOOSE_SKILLTREE_ONLY_ONCE && !myPet.getOwner().isMyPetAdmin())
            {
                sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_OnlyChooseSkilltreeOnce").replace("%petname%", myPet.petName)));
            }
            else if (MyPetSkillTreeMobType.hasMobType(myPet.getPetType().getTypeName()))
            {
                MyPetSkillTreeMobType skillTreeMobType = MyPetSkillTreeMobType.getMobTypeByName(myPet.getPetType().getTypeName());
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
                        MyPetSkillTree skillTree = skillTreeMobType.getSkillTree(skilltreeName);
                        if (MyPetPermissions.has(myPet.getOwner().getPlayer(), "MyPet.custom.skilltree." + skillTree.getPermission()))
                        {
                            if (myPet.setSkilltree(skillTree))
                            {
                                sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_SkilltreeSwitchedTo").replace("%name%", skillTree.getName())));
                            }
                            else
                            {
                                sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_SkilltreeNotSwitched")));
                            }
                        }
                        else
                        {
                            sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_CantFindSkilltree").replace("%name%", skilltreeName)));
                        }
                    }
                    else
                    {
                        sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_CantFindSkilltree").replace("%name%", skilltreeName)));
                    }
                }
                else
                {
                    sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_AvailableSkilltrees").replace("%petname%", myPet.petName)));
                    for (MyPetSkillTree skillTree : skillTreeMobType.getSkillTrees())
                    {
                        if (MyPetPermissions.has(player, "MyPet.custom.skilltree." + skillTree.getPermission()))
                        {
                            sender.sendMessage("   " + skillTree.getName());
                        }
                    }
                }
            }
        }
        else
        {
            sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_DontHavePet")));
        }
        return true;
    }
}
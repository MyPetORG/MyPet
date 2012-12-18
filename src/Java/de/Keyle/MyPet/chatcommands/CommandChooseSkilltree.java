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

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.skill.MyPetSkillTree;
import de.Keyle.MyPet.skill.MyPetSkillTreeMobType;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetList;
import de.Keyle.MyPet.util.MyPetPermissions;
import de.Keyle.MyPet.util.MyPetUtil;
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
            if (MyPetSkillTreeMobType.hasMobType(myPet.getPetType().getTypeName()))
            {
                MyPetSkillTreeMobType skillTreeMobType = MyPetSkillTreeMobType.getMobTypeByName(myPet.getPetType().getTypeName());
                if (args.length == 1)
                {
                    if (skillTreeMobType.hasSkillTree(args[0]))
                    {
                        MyPetSkillTree skillTree = skillTreeMobType.getSkillTree(args[0]);
                        if (MyPetPermissions.has(myPet.getOwner().getPlayer(), "MyPet.custom.skilltree." + skillTree.getName()))
                        {
                            myPet.setSkilltree(skillTree);
                        }
                        else
                        {
                            sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_CantFindSkilltree").replace("%name%", args[0])));
                        }
                    }
                    else
                    {
                        sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_CantFindSkilltree").replace("%name%", args[0])));
                    }
                }
                else
                {
                    sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_AvailableSkilltrees").replace("%petname%", myPet.petName)));
                    for (String skillTreeName : skillTreeMobType.getSkillTreeNames())
                    {
                        if (MyPetPermissions.has(player, "MyPet.custom.skilltree." + skillTreeName))
                        {
                            sender.sendMessage("   " + skillTreeName);
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
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

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.skill.MyPetGenericSkill;
import de.Keyle.MyPet.util.*;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;

public class CommandAdmin implements CommandExecutor
{
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player)
        {
            Player player = (Player) sender;
            if (!MyPetPermissions.has(player, "MyPet.admin", false))
            {
                return true;
            }
            if (args.length < 1)
            {
                return false;
            }
            String option = args[0];

            if (option.equalsIgnoreCase("name") && args.length >= 3)
            {
                String petOwner = args[1];
                if (!MyPetList.hasMyPet(petOwner))
                {
                    sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_UserDontHavePet").replace("%playername%", petOwner)));
                    return true;
                }
                MyPet myPet = MyPetList.getMyPet(petOwner);

                String name = "";
                for (int i = 2 ; i < args.length ; i++)
                {
                    name += args[i] + " ";
                }
                name = name.substring(0, name.length() - 1);
                myPet.setPetName(name);
            }
            else if (option.equalsIgnoreCase("exp") && args.length >= 3)
            {
                String petOwner = args[1];
                if (!MyPetList.hasMyPet(petOwner))
                {
                    sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_UserDontHavePet").replace("%playername%", petOwner)));
                    return true;
                }
                MyPet myPet = MyPetList.getMyPet(petOwner);
                String value = args[2];
                if (MyPetUtil.isInt(value))
                {
                    int Exp = Integer.parseInt(value);
                    Exp = Exp < 0 ? 0 : Exp;

                    Collection<MyPetGenericSkill> skills = myPet.getSkills().getSkills();
                    for (MyPetGenericSkill skill : skills)
                    {
                        skill.reset();
                    }
                    myPet.getExperience().reset();
                    myPet.getExperience().addExp(Exp);
                }
            }
            else if (option.equalsIgnoreCase("respawn"))
            {
                String petOwner = args[1];
                if (!MyPetList.hasMyPet(petOwner))
                {
                    sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_UserDontHavePet").replace("%playername%", petOwner)));
                    return true;
                }
                MyPet myPet = MyPetList.getMyPet(petOwner);
                if (myPet.getStatus() == PetState.Dead)
                {
                    if (args.length >= 3 && MyPetUtil.isInt(args[2]))
                    {
                        int respawnTime = Integer.parseInt(args[2]);
                        if (respawnTime >= 0)
                        {
                            myPet.respawnTime = respawnTime;
                        }
                    }
                    else
                    {
                        myPet.respawnTime = 0;
                    }
                }
            }
            else if (option.equalsIgnoreCase("reload"))
            {
                MyPetConfiguration.loadConfiguration();
                MyPetLogger.write("Config reloaded.");
                sender.sendMessage(MyPetUtil.setColors(ChatColor.AQUA + "MyPet config reloaded!"));
            }
            return true;
        }
        return true;
    }
}
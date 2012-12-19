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
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.skill.skills.MyPetGenericSkill;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetList;
import de.Keyle.MyPet.util.MyPetPermissions;
import de.Keyle.MyPet.util.MyPetUtil;
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
            if (!MyPetPermissions.has(player, "MyPet.admin"))
            {
                return true;
            }
            if (args.length < 2)
            {
                return false;
            }
            String petOwner = args[0];
            String change = args[1];

            if (!MyPetList.hasMyPet(petOwner))
            {
                sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_UserDontHavePet").replace("%playername%", petOwner)));
                return true;
            }
            MyPet myPet = MyPetList.getMyPet(petOwner);
            if (change.equalsIgnoreCase("name") && args.length >= 3)
            {
                String name = "";
                for (int i = 2 ; i < args.length ; i++)
                {
                    name += args[i] + " ";
                }
                name = name.substring(0, name.length() - 1);
                myPet.setPetName(name);
            }
            else if (change.equalsIgnoreCase("exp") && args.length >= 3)
            {
                String value = args[2];
                if (MyPetUtil.isInt(value))
                {
                    int Exp = Integer.parseInt(value);
                    Exp = Exp < 0 ? 0 : Exp;

                    Collection<MyPetGenericSkill> skills = myPet.getSkills().getSkills();
                    if (skills.size() > 0)
                    {
                        for (MyPetGenericSkill skill : skills)
                        {
                            skill.setLevel(0);
                        }
                    }
                    myPet.getExperience().reset();
                    myPet.getExperience().addExp(Exp);
                }
            }
            else if (change.equalsIgnoreCase("respawn"))
            {
                if (myPet.status == PetState.Dead)
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
            return true;
        }
        return true;
    }
}
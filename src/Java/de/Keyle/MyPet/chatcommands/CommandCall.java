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

import de.Keyle.MyPet.entity.EntitySize;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.event.MyPetSpoutEvent;
import de.Keyle.MyPet.event.MyPetSpoutEvent.MyPetSpoutEventReason;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetList;
import de.Keyle.MyPet.util.MyPetUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static org.bukkit.Bukkit.getPluginManager;

public class CommandCall implements CommandExecutor
{
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player)
        {
            Player petOwner = (Player) sender;
            if (MyPetList.hasMyPet(petOwner))
            {
                MyPet myPet = MyPetList.getMyPet(petOwner);
                if (myPet.getStatus() == PetState.Here)
                {
                    if (myPet.getCraftPet().isInsideVehicle())
                    {
                        myPet.getCraftPet().leaveVehicle();
                    }

                    myPet.removePet();
                    if (MyPetUtil.canSpawn(petOwner.getLocation(), myPet.getCraftPet().getHandle()))
                    {
                        myPet.setLocation(petOwner.getLocation());
                        if (myPet.createPet())
                        {
                            sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_Call")).replace("%petname%", myPet.petName));
                            getPluginManager().callEvent(new MyPetSpoutEvent(myPet, MyPetSpoutEventReason.Call));
                        }
                        else
                        {
                            sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_SpawnPrevent")).replace("%petname%", myPet.petName));
                        }
                    }
                    else
                    {
                        sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_SpawnNoSpace")).replace("%petname%", myPet.petName));
                    }
                    return true;
                }
                else if (myPet.getStatus() == PetState.Despawned)
                {
                    EntitySize es = myPet.getPetType().getEntityClass().getAnnotation(EntitySize.class);
                    if (es != null && MyPetUtil.canSpawn(petOwner.getLocation(), es.height(), 0.0F, es.width()))
                    {
                        myPet.setLocation(petOwner.getLocation());
                        if (myPet.createPet())
                        {
                            sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_Call")).replace("%petname%", myPet.petName));
                            getPluginManager().callEvent(new MyPetSpoutEvent(myPet, MyPetSpoutEventReason.Call));
                        }
                        else
                        {
                            sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_SpawnPrevent")).replace("%petname%", myPet.petName));
                        }
                    }
                    else
                    {
                        sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_SpawnNoSpace")).replace("%petname%", myPet.petName));
                    }
                    return true;
                }
                else if (myPet.getStatus() == PetState.Dead)
                {
                    sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_CallDead")).replace("%petname%", myPet.petName).replace("%time%", "" + myPet.respawnTime));
                    return true;
                }
            }
            else
            {
                sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_DontHavePet")));
            }
        }
        return true;
    }
}
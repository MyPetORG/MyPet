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
import de.Keyle.MyPet.event.MyPetSpoutEvent;
import de.Keyle.MyPet.event.MyPetSpoutEvent.MyPetSpoutEventReason;
import de.Keyle.MyPet.util.MyPetConfiguration;
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
            if (MyPetList.hasActiveMyPets(petOwner))
            {
                for (MyPet myPet : MyPetList.getActiveMyPets(petOwner))
                {
                    myPet.removePet();
                    myPet.setLocation(petOwner.getLocation());

                    switch (myPet.createPet())
                    {
                        case Success:
                            if (MyPetConfiguration.ENABLE_EVENTS)
                            {
                                sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_Call")).replace("%petname%", myPet.petName));
                                getPluginManager().callEvent(new MyPetSpoutEvent(myPet, MyPetSpoutEventReason.Call));
                            }
                            break;
                        case Canceled:
                            sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_SpawnPrevent")).replace("%petname%", myPet.petName));
                            break;
                        case NoSpace:
                            sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_SpawnNoSpace")).replace("%petname%", myPet.petName));
                            break;
                        case Dead:
                            sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_CallDead")).replace("%petname%", myPet.petName).replace("%time%", "" + myPet.respawnTime));
                            break;
                    }
                }
                return true;
            }
            else
            {
                sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_DontHavePet")));
            }
            return true;
        }
        sender.sendMessage("You can't use this command from server console!");
        return true;
    }
}
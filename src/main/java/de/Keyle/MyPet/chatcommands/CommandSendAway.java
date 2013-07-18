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

import de.Keyle.MyPet.api.event.MyPetSpoutEvent;
import de.Keyle.MyPet.api.event.MyPetSpoutEvent.MyPetSpoutEventReason;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.entity.types.MyPetList;
import de.Keyle.MyPet.util.MyPetConfiguration;
import de.Keyle.MyPet.util.MyPetUtil;
import de.Keyle.MyPet.util.locale.MyPetLocales;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static org.bukkit.Bukkit.getPluginManager;

public class CommandSendAway implements CommandExecutor
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
                    myPet.removePet(false);
                    sender.sendMessage(MyPetUtil.formatText(MyPetLocales.getString("Message.SendAway", petOwner), myPet.getPetName()));
                    if (MyPetConfiguration.ENABLE_EVENTS)
                    {
                        getPluginManager().callEvent(new MyPetSpoutEvent(myPet, MyPetSpoutEventReason.SendAway));
                    }
                }
                else if (myPet.getStatus() == PetState.Despawned)
                {
                    sender.sendMessage(MyPetUtil.formatText(MyPetLocales.getString("Message.AlreadyAway", petOwner), myPet.getPetName()));
                }
                else if (myPet.getStatus() == PetState.Dead)
                {
                    sender.sendMessage(MyPetUtil.formatText(MyPetLocales.getString("Message.CallWhenDead", petOwner), myPet.getPetName(), myPet.getRespawnTime()));
                }
            }
            else
            {
                sender.sendMessage(MyPetLocales.getString("Message.DontHavePet", petOwner));
            }
            return true;
        }
        sender.sendMessage("You can't use this command from server console!");
        return true;
    }
}
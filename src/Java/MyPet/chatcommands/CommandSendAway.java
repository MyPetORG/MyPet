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

package Java.MyPet.chatcommands;

import Java.MyPet.util.MyPetLanguage;
import Java.MyPet.util.MyPetUtil;
import Java.MyPet.entity.types.MyPet;
import Java.MyPet.entity.types.MyPet.PetState;
import Java.MyPet.event.MyPetSpoutEvent;
import Java.MyPet.event.MyPetSpoutEvent.MyPetSpoutEventReason;
import Java.MyPet.util.MyPetList;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
                if (myPet.status == PetState.Here)
                {
                    myPet.removePet();
                    sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_SendAway")).replace("%petname%", myPet.petName));
                    MyPetUtil.getServer().getPluginManager().callEvent(new MyPetSpoutEvent(myPet, MyPetSpoutEventReason.SendAway));
                    return true;
                }
                else if (myPet.status == PetState.Despawned)
                {
                    sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_AlreadyAway")).replace("%petname%", myPet.petName));
                    return true;
                }
                else if (myPet.status == PetState.Dead)
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
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

import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.entity.types.wolf.MyWolf;
import de.Keyle.MyPet.event.MyPetSpoutEvent;
import de.Keyle.MyPet.event.MyPetSpoutEvent.MyPetSpoutEventReason;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetList;
import de.Keyle.MyPet.util.MyPetPermissions;
import de.Keyle.MyPet.util.MyPetUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandCall implements CommandExecutor
{
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player)
        {
            Player owner = (Player) sender;
            if (MyPetList.hasMyPet(owner))
            {
                MyWolf MPet = MyPetList.getMyPet(owner);
                if (!MyPetPermissions.has(owner, "MyPet.user.call"))
                {
                    return true;
                }
                if (MPet.Status == PetState.Here)
                {
                    if (MPet.getLocation().getWorld() != owner.getLocation().getWorld())
                    {
                        MPet.removePet();
                        MPet.setLocation(owner.getLocation());
                        MPet.createPet(false);
                    }
                    else
                    {
                        if (MPet.Wolf.isInsideVehicle())
                        {
                            MPet.Wolf.leaveVehicle();
                        }
                        MPet.Wolf.teleport(owner);
                    }
                    sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_Call")).replace("%petname%", MPet.Name));
                    MyPetUtil.getServer().getPluginManager().callEvent(new MyPetSpoutEvent(MPet, MyPetSpoutEventReason.Call));
                    return true;
                }
                else if (MPet.Status == PetState.Despawned)
                {
                    MPet.setLocation(owner.getLocation());
                    MPet.createPet(false);
                    sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_Call")).replace("%petname%", MPet.Name));
                    MyPetUtil.getServer().getPluginManager().callEvent(new MyPetSpoutEvent(MPet, MyPetSpoutEventReason.Call));
                    return true;
                }
                else if (MPet.Status == PetState.Dead)
                {
                    sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_CallDead")).replace("%petname%", MPet.Name).replace("%time%", "" + MPet.RespawnTime));
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
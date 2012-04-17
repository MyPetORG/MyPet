/*
 * Copyright (C) 2011-2012 Keyle
 *
 * This file is part of MyWolf
 *
 * MyWolf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyWolf is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyWolf. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyWolf.chatcommands;

import de.Keyle.MyWolf.MyWolf;
import de.Keyle.MyWolf.MyWolf.WolfState;
import de.Keyle.MyWolf.event.MyWolfSpoutEvent;
import de.Keyle.MyWolf.event.MyWolfSpoutEvent.MyWolfSpoutEventReason;
import de.Keyle.MyWolf.util.MyWolfLanguage;
import de.Keyle.MyWolf.util.MyWolfList;
import de.Keyle.MyWolf.util.MyWolfPermissions;
import de.Keyle.MyWolf.util.MyWolfUtil;
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
            if (MyWolfList.hasMyWolf(owner))
            {
                MyWolf MWolf = MyWolfList.getMyWolf(owner);
                if (!MyWolfPermissions.has(owner, "MyWolf.user.call"))
                {
                    return true;
                }
                if (MWolf.Status == WolfState.Here)
                {
                    if (MWolf.getLocation().getWorld() != owner.getLocation().getWorld())
                    {
                        MWolf.removeWolf();
                        MWolf.setLocation(owner.getLocation());
                        MWolf.createWolf(false);
                    }
                    else
                    {
                        if (MWolf.Wolf.isInsideVehicle())
                        {
                            MWolf.Wolf.leaveVehicle();
                        }
                        MWolf.Wolf.teleport(owner);
                    }
                    sender.sendMessage(MyWolfUtil.setColors(MyWolfLanguage.getString("Msg_Call")).replace("%wolfname%", MWolf.Name));
                    MyWolfUtil.getServer().getPluginManager().callEvent(new MyWolfSpoutEvent(MWolf, MyWolfSpoutEventReason.Call));
                    return true;
                }
                else if (MWolf.Status == WolfState.Despawned)
                {
                    MWolf.setLocation(owner.getLocation());
                    MWolf.createWolf(false);
                    sender.sendMessage(MyWolfUtil.setColors(MyWolfLanguage.getString("Msg_Call")).replace("%wolfname%", MWolf.Name));
                    MyWolfUtil.getServer().getPluginManager().callEvent(new MyWolfSpoutEvent(MWolf, MyWolfSpoutEventReason.Call));
                    return true;
                }
                else if (MWolf.Status == WolfState.Dead)
                {
                    sender.sendMessage(MyWolfUtil.setColors(MyWolfLanguage.getString("Msg_CallDead")).replace("%wolfname%", MWolf.Name).replace("%time%", "" + MWolf.RespawnTime));
                    return true;
                }
            }
            else
            {
                sender.sendMessage(MyWolfUtil.setColors(MyWolfLanguage.getString("Msg_DontHaveWolf")));
            }
        }
        return true;
    }
}

/*
* Copyright (C) 2011-2012 Keyle
*
* This file is part of MyWolf.
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

import de.Keyle.MyWolf.ConfigBuffer;
import de.Keyle.MyWolf.MyWolf;
import de.Keyle.MyWolf.MyWolf.WolfState;
import de.Keyle.MyWolf.util.MyWolfLanguage;
import de.Keyle.MyWolf.util.MyWolfPermissions;
import de.Keyle.MyWolf.util.MyWolfUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MyWolfCompass implements CommandExecutor
{
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player)
        {
            Player player = (Player) sender;
            if (ConfigBuffer.mWolves.containsKey(player.getName()))
            {
                MyWolf Wolf = ConfigBuffer.mWolves.get(player.getName());

                if (!MyWolfPermissions.has(Wolf.getOwner(), "MyWolf.compass"))
                {
                    return true;
                }

                if (Wolf.Status == WolfState.Dead)
                {
                    sender.sendMessage(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_CallDead")).replace("%wolfname%", ConfigBuffer.mWolves.get(player.getName()).Name).replace("%time%", "" + ConfigBuffer.mWolves.get(player.getName()).RespawnTime));
                    return true;
                }
                else
                {
                    if (args.length == 1 && args[0].equalsIgnoreCase("Stop"))
                    {
                        Wolf.getOwner().setCompassTarget(Wolf.getLocation().getWorld().getSpawnLocation());
                    }
                    else if (args.length > 1)
                    {
                        return false;
                    }
                    else
                    {
                        Wolf.getOwner().setCompassTarget(Wolf.getLocation());
                    }
                    sender.sendMessage("Your compass points now to the last known position of " + Wolf.Name);
                }
                return true;
            }
            else
            {
                sender.sendMessage(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_DontHaveWolf")));
            }
        }
        return true;
    }
}

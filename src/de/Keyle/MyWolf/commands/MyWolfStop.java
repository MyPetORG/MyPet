/*
* Copyright (C) 2011 Keyle
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

package de.Keyle.MyWolf.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import de.Keyle.MyWolf.ConfigBuffer;

public class MyWolfStop implements CommandExecutor {

    private ConfigBuffer cb;

	public MyWolfStop(ConfigBuffer cb)
	{
	  this.cb = cb;
	}

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player)
        {
    		Player player = (Player) sender;
    		if(cb.mWolves.containsKey(player.getName()))
    		{
	    		if(cb.Permissions.has(player, "mywolf.stop") == false)
				{
					return true;
				}
				if(cb.mWolves.get(player.getName()).isDead == true || cb.mWolves.get(player.getName()).isThere == false)
	    		{
					sender.sendMessage("You must call your wolf first.");
					return true;
				}
				sender.sendMessage("Your wolf should now " + ChatColor.GREEN + "stop" + ChatColor.WHITE + " attacking!");
				cb.mWolves.get(player.getName()).MyWolf.setTarget((LivingEntity)null);
				return true;
    		}
    		else
    		{
    			sender.sendMessage("You don't have a wolf!");
    		}
        }
		return true;
    }
}

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

import net.minecraft.server.ItemStack;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.Keyle.MyWolf.ConfigBuffer;
import de.Keyle.MyWolf.util.MyWolfUtil;

public class MyWolfRelease implements CommandExecutor {

    private ConfigBuffer cb;

	public MyWolfRelease(ConfigBuffer cb)
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
	    		if(cb.Permissions.has(player, "mywolf.release") == false)
				{
					return true;
				}
				if(cb.mWolves.get(player.getName()).isDead == true || cb.mWolves.get(player.getName()).isThere == false)
	    		{
					sender.sendMessage(MyWolfUtil.SetColors(cb.lv.Msg_CallFirst));
					return true;
				}
				if(args.length < 1)
				{
					return false;
				}
				String name = "";
				for ( String arg : args )
				{
					name += arg + " ";
				}
				name = name.substring(0,name.length()-1);
				if(cb.mWolves.get(player.getName()).Name.equalsIgnoreCase(name))
				{
					cb.mWolves.get(player.getName()).MyWolf.setOwner(null);
					cb.mWolves.get(player.getName()).StopDropTimer();
					for(ItemStack is : cb.mWolves.get(player.getName()).LargeInventory.getContents())
					{
						if(is != null)
						{
							cb.mWolves.get(player.getName()).MyWolf.getWorld().dropItem(cb.mWolves.get(player.getName()).getLocation(), new org.bukkit.inventory.ItemStack(is.id, is.count, (short)is.damage));
						}
					}
					sender.sendMessage(MyWolfUtil.SetColors(cb.lv.Msg_Release).replace("%wolfname%", cb.mWolves.get(player.getName()).Name));
					//player.sendMessage(ChatColor.AQUA + cb.mWolves.get(player.getName()).Name + ChatColor.WHITE + " is now " + ChatColor.GREEN + "free" + ChatColor.WHITE + " . . .");
					cb.mWolves.remove(player.getName());
					cb.Plugin.SaveWolves();
					return true;
				}
				else
				{
					sender.sendMessage(MyWolfUtil.SetColors(cb.lv.Msg_Name).replace("%wolfname%", cb.mWolves.get(player.getName()).Name));
					//player.sendMessage("The name of your wolf is: " + ChatColor.AQUA + cb.mWolves.get(player.getName()).Name);
					return false;
				}
    		}
			else
			{
				sender.sendMessage(MyWolfUtil.SetColors(cb.lv.Msg_DontHaveWolf));
			}
        }
		return false;
    }
}

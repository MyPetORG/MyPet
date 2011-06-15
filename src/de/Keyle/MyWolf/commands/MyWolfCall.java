package de.Keyle.MyWolf.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.Keyle.MyWolf.ConfigBuffer;

public class MyWolfCall implements CommandExecutor
{

    private ConfigBuffer cb;

	public MyWolfCall(ConfigBuffer cb)
	{
	  this.cb = cb;
	}

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player)
        {
    		Player player = (Player)sender;
    		if(cb.mWolves.containsKey(player.getName()))
    		{
		    	if(cb.Permissions.has(player, "mywolf.call") == false)
				{
					return false;
				}
				cb.mWolves.get(player.getName()).Location = player.getLocation();
				if(cb.mWolves.get(player.getName()).isThere == true && cb.mWolves.get(player.getName()).isDead == false)
				{
					cb.mWolves.get(player.getName()).MyWolf.teleport(player);
					cb.mWolves.get(player.getName()).Location = player.getLocation();
					player.sendMessage(ChatColor.AQUA+cb.mWolves.get(player.getName()).Name + ChatColor.WHITE + " comes to you.");
				}
				else if(cb.mWolves.get(player.getName()).isThere == false && cb.mWolves.get(player.getName()).RespawnTime == 0)
				{
					cb.mWolves.get(player.getName()).Location = player.getLocation();
					cb.mWolves.get(player.getName()).createWolf(false);
					player.sendMessage(ChatColor.AQUA+cb.mWolves.get(player.getName()).Name + ChatColor.WHITE + " comes to you.");
				}
				else if(cb.mWolves.get(player.getName()).isDead == true)
				{
					player.sendMessage(ChatColor.AQUA+cb.mWolves.get(player.getName()).Name + ChatColor.WHITE + " is dead! and respawns in "+ChatColor.GOLD+cb.mWolves.get(player.getName()).RespawnTime+ChatColor.WHITE +" sec");
					return false;
				}
	        }
			else
			{
				sender.sendMessage("You don't have a wolf!");
			}
        }
		return true;
    }
}

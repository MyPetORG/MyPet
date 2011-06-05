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
    		if(cb.mWolfs.containsKey(player.getName()))
    		{
		    	if(cb.Permissions.has(player, "mywolf.call") == false)
				{
					return false;
				}
				cb.mWolfs.get(player.getName()).WolfLocation = player.getLocation();
				if(cb.mWolfs.get(player.getName()).isThere == true)
				{
					cb.mWolfs.get(player.getName()).MyWolf.teleport(player.getLocation());
					cb.mWolfs.get(player.getName()).WolfLocation = player.getLocation();
					player.sendMessage(ChatColor.AQUA+cb.mWolfs.get(player.getName()).Name + ChatColor.WHITE + " is coming to you.");
				}
				else if(cb.mWolfs.get(player.getName()).isThere == false && cb.mWolfs.get(player.getName()).RespawnTime == 0)
				{
					cb.mWolfs.get(player.getName()).WolfLocation = player.getLocation();
					cb.mWolfs.get(player.getName()).createWolf(false);
					player.sendMessage(ChatColor.AQUA+cb.mWolfs.get(player.getName()).Name + ChatColor.WHITE + " is coming to you.");
				}
				else if(cb.mWolfs.get(player.getName()).isDead == true)
				{
					player.sendMessage(ChatColor.AQUA+cb.mWolfs.get(player.getName()).Name + ChatColor.WHITE + " is dead!");
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

package de.Keyle.MyWolf.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.Keyle.MyWolf.ConfigBuffer;

public class MyWolfName implements CommandExecutor {

    private ConfigBuffer cb;

	public MyWolfName(ConfigBuffer cb)
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
	    		if(cb.Permissions.has(player, "mywolf.setname") == false)
				{
					return false;
				}
				if(args.length < 1)
				{
					player.sendMessage("Please enter the name of the wolf.");
					player.sendMessage("Syntax: /wolf name "+ChatColor.AQUA+"<wolfname>");
					return false;
				}
				String name = ""; 
				for ( String arg : args )
				{
					name += arg + " ";
				}
				name = name.substring(0,name.length()-1);
				cb.mWolves.get(player.getName()).Name = name;
				player.sendMessage("The name of your wolf is now: " + ChatColor.AQUA + name);
				return true;
    		}
			else
			{
				sender.sendMessage("You don't have a wolf!");
			}
        }
		return false;
    }
}

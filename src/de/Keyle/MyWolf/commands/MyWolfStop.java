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
    		if(cb.mWolfs.containsKey(player.getName()))
    		{
	    		if(cb.Permissions.has(player, "mywolf.stop") == false)
				{
					return false;
				}
				if(cb.mWolfs.get(player.getName()).isDead == true || cb.mWolfs.get(player.getName()).isThere == false)
	    		{
					sender.sendMessage("You must call your wolf first.");
					return false;
				}
				sender.sendMessage("Your wolf should now " + ChatColor.GREEN + "stop" + ChatColor.WHITE + " attacking!");
				cb.mWolfs.get(player.getName()).MyWolf.setTarget((LivingEntity)null);
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

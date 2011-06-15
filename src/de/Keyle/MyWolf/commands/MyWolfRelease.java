package de.Keyle.MyWolf.commands;

import net.minecraft.server.ItemStack;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.Keyle.MyWolf.ConfigBuffer;

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
					return false;
				}
				if(cb.mWolves.get(player.getName()).isDead == true || cb.mWolves.get(player.getName()).isThere == false)
	    		{
					player.sendMessage("You must call your wolf first.");
					return false;
				}
				if(args.length < 1)
				{
					player.sendMessage("Please enter the name of the wolf.");
					player.sendMessage("Syntax: /wolf release "+ChatColor.AQUA+"<wolfname>");
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
					for(ItemStack is : cb.mWolves.get(player.getName()).Inventory.getContents())
					{
						if(is != null)
						{
							cb.mWolves.get(player.getName()).MyWolf.getWorld().dropItem(cb.mWolves.get(player.getName()).getLocation(), new org.bukkit.inventory.ItemStack(is.id, is.count, (short)is.damage));
						}
					}
					player.sendMessage(ChatColor.AQUA + cb.mWolves.get(player.getName()).Name + ChatColor.WHITE + " is now " + ChatColor.GREEN + "free" + ChatColor.WHITE + " . . .");
					cb.mWolves.remove(player.getName());
					cb.Plugin.SaveWolves();
					return true;
				}
				else
				{
					player.sendMessage("Please enter the name of YOUR wolf.");
				}
    		}
			else
			{
				sender.sendMessage("You don't have a wolf!");
			}
        }
		return false;
    }
}

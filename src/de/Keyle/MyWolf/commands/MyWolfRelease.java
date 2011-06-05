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
    		if(cb.mWolfs.containsKey(player.getName()))
    		{
	    		if(cb.Permissions.has(player, "mywolf.release") == false)
				{
					return false;
				}
				if(cb.mWolfs.get(player.getName()).isDead == true || cb.mWolfs.get(player.getName()).isThere == false)
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
				if(cb.mWolfs.get(player.getName()).Name.equalsIgnoreCase(name))
				{
					cb.mWolfs.get(player.getName()).MyWolf.setOwner(null);
					for(ItemStack is : cb.mWolfs.get(player.getName()).WolfInventory.getContents())
					{
						if(is != null)
						{
							cb.mWolfs.get(player.getName()).MyWolf.getWorld().dropItem(cb.mWolfs.get(player.getName()).getLoc(), new org.bukkit.inventory.ItemStack(is.id, is.count, (short)is.damage));
						}
					}
					player.sendMessage(ChatColor.AQUA + cb.mWolfs.get(player.getName()).Name + ChatColor.WHITE + " is now " + ChatColor.GREEN + "free" + ChatColor.WHITE + " . . .");
					cb.mWolfs.remove(player.getName());
					cb.Plugin.SaveWolfs();
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

package de.Keyle.MyWolf.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MyWolfInfo implements CommandExecutor {

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player)
        {
    		Player player = (Player) sender;
    		player.sendMessage("MyWolf - Help - - - - - - - - - - - - - - - - -");
			player.sendMessage("Set wolf name:            /wolf name <newwolfname>");
			player.sendMessage("Release your wolf:    /wolf release <wolfname>");
			player.sendMessage("Stop wolf attacking:  /wolf stop  (alias: /ws)");
			player.sendMessage("Call your wolf:	          /wolf call  (alias: /wc)");
			//player.sendMessage("Compass tagets wolf:	          /wolf compass [stop]");
        }
		return true;
    }
}

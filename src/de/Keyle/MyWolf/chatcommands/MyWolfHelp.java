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

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MyWolfHelp implements CommandExecutor
{

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player)
        {
            Player player = (Player) sender;
            player.sendMessage("MyWolf - Help - - - - - - - - - - - - - - - - -");
            player.sendMessage("Set wolf name:            /wolfname <newwolfname>");
            player.sendMessage("Release your wolf:    /wolfrelease <wolfname>");
            player.sendMessage("Stop wolf attacking:  /wolfstop  (alias: /ws or /wolfs)");
            player.sendMessage("Call your wolf:	          /wolfcall  (alias: /wc or /wolfc)");
            player.sendMessage("Display the EXP:	          /wolfexp  (alias: /we or /wolfe)");
            player.sendMessage("Display wolf info:	          /wolfinfo  (alias: /winfo)");
            player.sendMessage("Toggle the behaivior:	          /wolfbehavior [RAID]   (alias: /wb or /wolfb)");
            player.sendMessage("Toggle wolf pickup on/off:	          /wolfpickup   (alias: /wp or /wolfp)");
            player.sendMessage("Open the inventory of the wolf:	          /wolfinventory   (alias: /wi or /wolfi)");
            player.sendMessage("Compass tagets wolf:	          /wolfcompass [stop]");
            player.sendMessage("Changes wolf skin:	          /wolfskin <URL>");
            player.sendMessage("- - - - - - - - - - - - - - - - - - - - - - - -");
        }
        return true;
    }
}

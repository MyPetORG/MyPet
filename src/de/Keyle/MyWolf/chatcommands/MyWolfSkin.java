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

package de.Keyle.MyWolf.chatcommands;

import de.Keyle.MyWolf.ConfigBuffer;
import de.Keyle.MyWolf.util.MyWolfLanguage;
import de.Keyle.MyWolf.util.MyWolfPermissions;
import de.Keyle.MyWolf.util.MyWolfUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MyWolfSkin implements CommandExecutor
{
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player)
        {
            Player player = (Player) sender;
            if(args.length == 2)
            {
                if (!MyWolfPermissions.has(player, "MyWolf.skin.other"))
                {
                    return true;
                }
                if (ConfigBuffer.mWolves.containsKey(args[0]))
                {
                    ConfigBuffer.mWolves.get(args[0]).setTameSkin(args[1]);
                }
                else
                {
                    sender.sendMessage(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_UserDontHaveWolf")));
                }
            }
            else if (ConfigBuffer.mWolves.containsKey(player.getName()))
            {
                if (!MyWolfPermissions.has(player, "MyWolf.skin.self"))
                {
                    return true;
                }
                if (args.length == 1)
                {
                    ConfigBuffer.mWolves.get(player.getName()).setTameSkin(args[0]);
                }
                return true;
            }
            else
            {
                sender.sendMessage(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_DontHaveWolf")));
                return true;
            }

        }
        return true;
    }
}

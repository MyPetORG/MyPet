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
import de.Keyle.MyWolf.MyWolf;
import de.Keyle.MyWolf.MyWolf.WolfState;
import de.Keyle.MyWolf.MyWolfPlugin;
import de.Keyle.MyWolf.util.MyWolfLanguage;
import de.Keyle.MyWolf.util.MyWolfPermissions;
import de.Keyle.MyWolf.util.MyWolfUtil;
import net.minecraft.server.ItemStack;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MyWolfRelease implements CommandExecutor
{
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player)
        {
            Player player = (Player) sender;
            if (ConfigBuffer.mWolves.containsKey(player.getName()))
            {
                MyWolf Wolf = ConfigBuffer.mWolves.get(player.getName());

                if (!MyWolfPermissions.has(player, "MyWolf.release"))
                {
                    return true;
                }
                if (Wolf.Status == WolfState.Despawned)
                {
                    sender.sendMessage(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_CallFirst")));
                    return true;
                }
                if (args.length < 1)
                {
                    return false;
                }
                String name = "";
                for (String arg : args)
                {
                    name += arg + " ";
                }
                name = name.substring(0, name.length() - 1);
                if (Wolf.Name.equalsIgnoreCase(name))
                {
                    Wolf.Wolf.setOwner(null);
                    Wolf.StopTimer();
                    for (ItemStack is : Wolf.inv.getContents())
                    {
                        if (is != null)
                        {
                            Wolf.Wolf.getWorld().dropItem(Wolf.getLocation(), new org.bukkit.inventory.ItemStack(is.id, is.count, (short) is.getData()));
                        }
                    }
                    sender.sendMessage(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_Release")).replace("%wolfname%", ConfigBuffer.mWolves.get(player.getName()).Name));
                    ConfigBuffer.mWolves.remove(player.getName());
                    MyWolfPlugin.Plugin.SaveWolves(ConfigBuffer.WolvesConfig);
                    return true;
                }
                else
                {
                    sender.sendMessage(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_Name")).replace("%wolfname%", ConfigBuffer.mWolves.get(player.getName()).Name));
                    return false;
                }
            }
            else
            {
                sender.sendMessage(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_DontHaveWolf")));
            }
        }
        return false;
    }
}

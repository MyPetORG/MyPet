/*
 * Copyright (C) 2011-2012 Keyle
 *
 * This file is part of MyWolf
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

import de.Keyle.MyWolf.MyWolf;
import de.Keyle.MyWolf.MyWolf.WolfState;
import de.Keyle.MyWolf.MyWolfPlugin;
import de.Keyle.MyWolf.util.MyWolfLanguage;
import de.Keyle.MyWolf.util.MyWolfList;
import de.Keyle.MyWolf.util.MyWolfPermissions;
import de.Keyle.MyWolf.util.MyWolfUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandRelease implements CommandExecutor
{
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player)
        {
            Player owner = (Player) sender;
            if (MyWolfList.hasMyWolf(owner))
            {
                MyWolf MWolf = MyWolfList.getMyWolf(owner);

                if (!MyWolfPermissions.has(owner, "MyWolf.user.release"))
                {
                    return true;
                }
                if (MWolf.Status == WolfState.Despawned)
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
                if (MWolf.Name.equalsIgnoreCase(name))
                {
                    MWolf.Wolf.setOwner(null);
                    MWolf.StopTimer();
                    /*
                    for (ItemStack is : MWolf.inv.getContents())
                    {
                        if (is != null)
                        {
                            MWolf.Wolf.getWorld().dropItem(MWolf.getLocation(), new org.bukkit.inventory.ItemStack(is.id, is.count, (short) is.getData()));
                        }
                    }
                    */
                    sender.sendMessage(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_Release")).replace("%wolfname%", MWolf.Name));
                    MyWolfList.removeMyWolf(MWolf);
                    MyWolfPlugin.Plugin.SaveWolves(MyWolfPlugin.MWWolvesConfig);
                    return true;
                }
                else
                {
                    sender.sendMessage(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_Name")).replace("%wolfname%", MWolf.Name));
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

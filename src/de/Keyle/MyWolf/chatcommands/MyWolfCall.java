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
import de.Keyle.MyWolf.util.MyWolfConfig;
import de.Keyle.MyWolf.util.MyWolfLanguage;
import de.Keyle.MyWolf.util.MyWolfPermissions;
import de.Keyle.MyWolf.util.MyWolfUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.getspout.spout.player.SpoutCraftPlayer;
import org.getspout.spoutapi.SpoutManager;

public class MyWolfCall implements CommandExecutor
{
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player)
        {
            Player player = (Player) sender;
            if (ConfigBuffer.mWolves.containsKey(player.getName()))
            {
                MyWolf Wolf = ConfigBuffer.mWolves.get(player.getName());
                sender.sendMessage(Wolf.Status.toString());
                if (!MyWolfPermissions.has(player, "MyWolf.call"))
                {
                    return true;
                }
                if (Wolf.Status == WolfState.Here)
                {
                    if(MyWolfConfig.SpoutSounds)
                    {
                        SpoutManager.getSoundManager().playCustomMusic(MyWolfPlugin.Plugin, SpoutCraftPlayer.getPlayer((Player) sender), "http://dl.dropbox.com/u/23957620/MinecraftPlugins/util/call.ogg", true);
                    }
                    if (Wolf.getLocation().getWorld() != player.getLocation().getWorld())
                    {
                        Wolf.removeWolf();
                        Wolf.setLocation(player.getLocation());
                        Wolf.createWolf(false);
                    }
                    else
                    {
                        Wolf.Wolf.teleport(player);
                    }
                    sender.sendMessage(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_Call")).replace("%wolfname%", ConfigBuffer.mWolves.get(player.getName()).Name));
                    return true;
                }
                else if (Wolf.Status == WolfState.Despawned)
                {

                    if(MyWolfConfig.SpoutSounds)
                    {
                        SpoutManager.getSoundManager().playCustomMusic(MyWolfPlugin.Plugin, SpoutCraftPlayer.getPlayer((Player) sender), "http://dl.dropbox.com/u/23957620/MinecraftPlugins/util/call.ogg", true);
                    }
                    Wolf.setLocation(player.getLocation());
                    Wolf.createWolf(false);
                    sender.sendMessage(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_Call")).replace("%wolfname%", ConfigBuffer.mWolves.get(player.getName()).Name));
                    return true;
                }
                else if (Wolf.Status == WolfState.Dead)
                {
                    sender.sendMessage(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_CallDead")).replace("%wolfname%", ConfigBuffer.mWolves.get(player.getName()).Name).replace("%time%", "" + ConfigBuffer.mWolves.get(player.getName()).RespawnTime));
                    return true;
                }
            }
            else
            {
                sender.sendMessage(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_DontHaveWolf")));
            }
        }
        return true;
    }
}

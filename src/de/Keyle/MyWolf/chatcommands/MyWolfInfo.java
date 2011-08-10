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
import de.Keyle.MyWolf.util.MyWolfConfig;
import de.Keyle.MyWolf.util.MyWolfLanguage;
import de.Keyle.MyWolf.util.MyWolfUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MyWolfInfo implements CommandExecutor
{
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player)
        {
            Player player = (Player) sender;
            if (ConfigBuffer.mWolves.containsKey(player.getName()))
            {
                MyWolf wolf = ConfigBuffer.mWolves.get(player.getName());
                String msg;
                if (wolf.getHealth() > wolf.HealthMax / 3 * 2)
                {
                    msg = "" + ChatColor.GREEN + wolf.getHealth() + ChatColor.WHITE + "/" + ChatColor.YELLOW + wolf.HealthMax + ChatColor.WHITE;
                }
                else if (wolf.getHealth() > wolf.HealthMax / 3)
                {
                    msg = "" + ChatColor.YELLOW + wolf.getHealth() + ChatColor.WHITE + "/" + ChatColor.YELLOW + wolf.HealthMax + ChatColor.WHITE;
                }
                else
                {
                    msg = "" + ChatColor.RED + wolf.getHealth() + ChatColor.WHITE + "/" + ChatColor.YELLOW + wolf.HealthMax + ChatColor.WHITE;
                }
                player.sendMessage(MyWolfUtil.SetColors("%wolfname% HP: %hp%").replace("%wolfname%", wolf.Name).replace("%hp%", msg));
                if (MyWolfConfig.LevelSystem)
                {
                    player.sendMessage(MyWolfUtil.SetColors("%wolfname% (Lv%lvl%) (%proz%%) EXP:%exp%/%reqexp%").replace("%wolfname%", wolf.Name).replace("%exp%", String.format("%1.2f", wolf.Experience.getExp())).replace("%lvl%", "" + wolf.Experience.getLevel()).replace("%reqexp%", String.format("%1.2f", wolf.Experience.getrequireEXP())).replace("%proz%", String.format("%1.2f", wolf.Experience.getExp() * 100 / wolf.Experience.getrequireEXP())));
                }
                return true;
            }
            else
            {
                sender.sendMessage(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_DontHaveWolf")));
            }
        }
        return true;
    }
}

/*
 * Copyright (C) 2011-2012 Keyle
 *
 * This file is part of MyPet
 *
 * MyPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyPet. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.chatcommands;

import de.Keyle.MyPet.entity.types.wolf.MyWolf;
import de.Keyle.MyPet.util.MyPetConfig;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetList;
import de.Keyle.MyPet.util.MyPetUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandInfo implements CommandExecutor
{
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player)
        {
            Player player = (Player) sender;
            String playerName = sender.getName();
            if (args != null && args.length > 0)
            {
                playerName = args[0];
            }

            if (MyPetList.hasMyPet(MyPetUtil.getOfflinePlayer(playerName)))
            {
                MyWolf MPet = MyPetList.getMyPet(MyPetUtil.getOfflinePlayer(playerName));
                String msg;
                if (MPet.getHealth() > MPet.getMaxHealth() / 3 * 2)
                {
                    msg = "" + ChatColor.GREEN + MPet.getHealth() + ChatColor.WHITE + "/" + ChatColor.YELLOW + MPet.getMaxHealth() + ChatColor.WHITE;
                }
                else if (MPet.getHealth() > MPet.getMaxHealth() / 3)
                {
                    msg = "" + ChatColor.YELLOW + MPet.getHealth() + ChatColor.WHITE + "/" + ChatColor.YELLOW + MPet.getMaxHealth() + ChatColor.WHITE;
                }
                else
                {
                    msg = "" + ChatColor.RED + MPet.getHealth() + ChatColor.WHITE + "/" + ChatColor.YELLOW + MPet.getMaxHealth() + ChatColor.WHITE;
                }
                player.sendMessage(MyPetUtil.setColors("%aqua%%petname%%white% HP: %hp%").replace("%petname%", MPet.Name).replace("%hp%", msg));
                if (MyPetConfig.LevelSystem)
                {
                    int lvl = MPet.getExperience().getLevel();
                    double EXP = MPet.getExperience().getCurrentExp();
                    double reqEXP = MPet.getExperience().getRequiredExp();
                    player.sendMessage(MyPetUtil.setColors("%aqua%%petname%%white% (Lv%lvl%) (%proz%%) EXP:%exp%/%reqexp%").replace("%petname%", MPet.Name).replace("%exp%", String.format("%1.2f", EXP)).replace("%lvl%", "" + lvl).replace("%reqexp%", String.format("%1.2f", reqEXP)).replace("%proz%", String.format("%1.2f", EXP * 100 / reqEXP)));
                }
                if (args != null && args.length > 0)
                {
                    player.sendMessage(MyPetUtil.setColors("Owner: %Owner%").replace("%Owner%", playerName));
                }
                return true;
            }
            else
            {
                if (args != null && args.length > 0)
                {
                    sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_UserDontHavePet").replace("%playername%", playerName)));
                }
                else
                {
                    sender.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_DontHavePet")));
                }
            }
        }
        return true;
    }
}
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
import de.Keyle.MyWolf.skill.MyWolfGenericSkill;
import de.Keyle.MyWolf.util.MyWolfLanguage;
import de.Keyle.MyWolf.util.MyWolfList;
import de.Keyle.MyWolf.util.MyWolfUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;

public class CommandSkill implements CommandExecutor
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

            if (MyWolfList.hasMyWolf(MyWolfUtil.getOfflinePlayer(playerName)))
            {
                MyWolf MWolf = MyWolfList.getMyWolf(MyWolfUtil.getOfflinePlayer(playerName));

                player.sendMessage(MyWolfUtil.SetColors("%aqua%%wolfname%%white%'s skills:").replace("%wolfname%", MWolf.Name));
                Collection<MyWolfGenericSkill> Skills = MWolf.SkillSystem.getSkills();
                if (Skills.size() > 0)
                {
                    for (MyWolfGenericSkill Skill : Skills)
                    {
                        if (Skill.getLevel() > 0)
                        {
                            player.sendMessage(MyWolfUtil.SetColors("%green%%skillname%%white% lv: %gold%%lvl%").replace("%skillname%", Skill.getName()).replace("%lvl%", "" + Skill.getLevel()));
                        }
                    }
                }
                return true;
            }
            else
            {
                if (args != null && args.length > 0)
                {
                    sender.sendMessage(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_UserDontHaveWolf").replace("%playername%", playerName)));
                }
                else
                {
                    sender.sendMessage(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_DontHaveWolf")));
                }
            }
        }
        return true;
    }
}

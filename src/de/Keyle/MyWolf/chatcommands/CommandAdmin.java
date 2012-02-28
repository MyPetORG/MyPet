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
import de.Keyle.MyWolf.Skill.MyWolfGenericSkill;
import de.Keyle.MyWolf.util.MyWolfLanguage;
import de.Keyle.MyWolf.util.MyWolfList;
import de.Keyle.MyWolf.util.MyWolfPermissions;
import de.Keyle.MyWolf.util.MyWolfUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;

public class CommandAdmin implements CommandExecutor
{
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player)
        {
            Player player = (Player) sender;
            if (!MyWolfPermissions.has(player, "MyWolf.admin"))
            {
                return true;
            }
            if (args.length < 3)
            {
                return false;
            }
            String Wolfowner = args[0];
            String Change = args[1];
            String Value = args[2];

            if(!MyWolfList.hasMyWolf(MyWolfUtil.getOfflinePlayer(Wolfowner)) && !MyWolfList.hasInactiveMyWolf(MyWolfUtil.getOfflinePlayer(Wolfowner)))
            {
                sender.sendMessage(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_OtherDontHaveWolf").replace("%playername%", Wolfowner)));
                return true;
            }
            if(Change.equalsIgnoreCase("name"))
            {
                String name = "";
                for (int i = 2; i<args.length; i++)
                {
                    name += args[i] + " ";
                }
                name = name.substring(0, name.length() - 1);
                if(MyWolfList.hasMyWolf(MyWolfUtil.getOfflinePlayer(Wolfowner)))
                {
                    MyWolfList.getMyWolf(MyWolfUtil.getOfflinePlayer(Wolfowner)).Name = name;
                }
                else
                {
                    MyWolfList.getInactiveMyWolf(MyWolfUtil.getOfflinePlayer(Wolfowner)).setName(name);
                }
            }
            else  if(Change.equalsIgnoreCase("exp"))
            {
                if(MyWolfUtil.isInt(Value))
                {
                    int Exp = Integer.parseInt(Value);
                    Exp = Exp<0?0:Exp;
                    if(MyWolfList.hasMyWolf(MyWolfUtil.getOfflinePlayer(Wolfowner)))
                    {
                        MyWolf MWolf = MyWolfList.getMyWolf(MyWolfUtil.getOfflinePlayer(Wolfowner));

                        Collection<MyWolfGenericSkill> Skills = MWolf.SkillSystem.getSkills();
                        if(Skills.size() > 0)
                        {
                            for(MyWolfGenericSkill Skill : Skills)
                            {
                                Skill.setLevel(0);
                            }
                        }
                        MWolf.Experience.reset();
                        MWolf.Experience.addExp(Exp);
                    }
                    else
                    {
                        MyWolfList.getInactiveMyWolf(MyWolfUtil.getOfflinePlayer(Wolfowner)).setExp(Exp);
                    }
                }
            }

            return true;

        }
        return true;
    }
}

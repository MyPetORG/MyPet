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

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.skill.MyPetGenericSkill;
import de.Keyle.MyPet.util.MyPetLanguage;
import de.Keyle.MyPet.util.MyPetList;
import de.Keyle.MyPet.util.MyPetUtil;
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

            if (MyPetList.hasMyPet(playerName))
            {
                MyPet MPet = MyPetList.getMyPet(playerName);
                player.sendMessage(MyPetUtil.setColors(MyPetLanguage.getString("Msg_Skills")).replace("%petname%", MPet.Name).replace("%skilltree%", (!MPet.getSkillTree().getName().equals("%+-%NoNe%-+%") ? MPet.getSkillTree().getName() : "None")));
                Collection<MyPetGenericSkill> skills = MPet.getSkillSystem().getSkills();
                if (skills.size() > 0)
                {
                    for (MyPetGenericSkill skill : skills)
                    {
                        if (skill.getLevel() > 0)
                        {
                            player.sendMessage(MyPetUtil.setColors("%green%%skillname%%white% lv: %gold%%lvl%").replace("%skillname%", skill.getName()).replace("%lvl%", "" + skill.getLevel()));
                        }
                    }
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
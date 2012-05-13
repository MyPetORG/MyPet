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

package de.Keyle.MyWolf.chatcommands;

import de.Keyle.MyWolf.entity.types.MyPet.PetState;
import de.Keyle.MyWolf.entity.types.wolf.MyWolf;
import de.Keyle.MyWolf.util.MyWolfLanguage;
import de.Keyle.MyWolf.util.MyWolfList;
import de.Keyle.MyWolf.util.MyWolfUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandPickup implements CommandExecutor
{
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player)
        {
            Player owner = (Player) sender;
            if (MyWolfList.hasMyWolf(owner))
            {
                MyWolf MWolf = MyWolfList.getMyWolf(owner);

                if (MWolf.Status == PetState.Despawned)
                {
                    sender.sendMessage(MyWolfUtil.setColors(MyWolfLanguage.getString("Msg_CallFirst")));
                    return true;
                }
                else if (MWolf.Status == PetState.Dead)
                {
                    sender.sendMessage(MyWolfUtil.setColors(MyWolfLanguage.getString("Msg_CallDead")).replace("%wolfname%", MWolf.Name).replace("%time%", "" + MWolf.RespawnTime));
                    return true;
                }
                if (MWolf.SkillSystem.hasSkill("Pickup"))
                {
                    MWolf.SkillSystem.getSkill("Pickup").activate();
                }
                return true;
            }
            else
            {
                sender.sendMessage(MyWolfUtil.setColors(MyWolfLanguage.getString("Msg_DontHaveWolf")));
            }
        }
        return true;
    }
}
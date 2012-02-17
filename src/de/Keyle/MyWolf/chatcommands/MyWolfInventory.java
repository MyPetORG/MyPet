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
import de.Keyle.MyWolf.Skill.MyWolfSkill;
import de.Keyle.MyWolf.util.MyWolfLanguage;
import de.Keyle.MyWolf.util.MyWolfList;
import de.Keyle.MyWolf.util.MyWolfPermissions;
import de.Keyle.MyWolf.util.MyWolfUtil;
import net.minecraft.server.EntityPlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class MyWolfInventory implements CommandExecutor
{
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player)
        {
            Player player = (Player) sender;
            if(args.length == 0)
            {
                if (MyWolfList.hasMyWolf(player))
                {
                    MyWolf MWolf = MyWolfList.getMyWolf(player);
                    if (MWolf.Status == WolfState.Despawned)
                    {
                        sender.sendMessage(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_CallFirst")));
                        return true;
                    }
                    else if (MWolf.Status == WolfState.Dead)
                    {
                        sender.sendMessage(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_CallDead")).replace("%wolfname%", MWolf.Name).replace("%time%", "" + MWolf.RespawnTime));
                        return true;
                    }
                    if (MyWolfSkill.hasSkill(MWolf.Abilities, "Inventory"))
                    {
                        MyWolfSkill.RegisteredSkills.get("Inventory").run(MWolf, null);
                    }
                    else
                    {
                        sender.sendMessage(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_NoInventory")).replace("%wolfname%", MWolf.Name));
                    }
                    return true;
                }
                else
                {
                    sender.sendMessage(MyWolfUtil.SetColors(MyWolfLanguage.getString("Msg_DontHaveWolf")));
                }
            }
            else if(args.length == 1 && MyWolfPermissions.has(player,"MyWolf.Skills.Inventory.admin"))
            {
                if (MyWolfList.hasMyWolf(player))
                {
                    EntityPlayer eh = ((CraftPlayer) player).getHandle();
                    eh.a(MyWolfList.getMyWolf(player).inv);
                }
            }
        }
        return true;
    }
}
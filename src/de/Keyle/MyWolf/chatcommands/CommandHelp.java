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

import de.Keyle.MyWolf.util.MyWolfList;
import de.Keyle.MyWolf.util.MyWolfPermissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHelp implements CommandExecutor
{

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player)
        {
            Player player = (Player) sender;
            player.sendMessage("--------------- MyWolf - Help -------------------------");
            player.sendMessage("/wolfinfo [player] | Display info about a MyWolf  (alias: /winfo)");
            if (MyWolfPermissions.has(player, "MyWolf.admin"))
            {
                player.sendMessage("/wolfadmin [PlayerName] name/exp [Value] | (alias: /ws or /wolfs)");
            }
            if (MyWolfList.hasMyWolf(player))
            {
                player.sendMessage("/wolfname <newwolfname> | Set wolf name");
                player.sendMessage("/wolfrelease <wolfname> | Release your wolf");
                player.sendMessage("/wolfstop | MyWolf stopps attacking  (alias: /ws or /wolfs)");
                player.sendMessage("/wolfcall | Call your wolf  (alias: /wc or /wolfc)");
                player.sendMessage("/wolfskill | Shows the skill-levels");

                if (MyWolfList.getMyWolf(player).SkillSystem.hasSkill("Inventory") && MyWolfList.getMyWolf(player).SkillSystem.getSkill("Inventory").getLevel() > 0)
                {
                    player.sendMessage("/wolfinventory | Open the inventory of the wolf  (alias: /wi or /wolfi)");
                }
                if (MyWolfList.getMyWolf(player).SkillSystem.hasSkill("Pickup") && MyWolfList.getMyWolf(player).SkillSystem.getSkill("Pickup").getLevel() > 0)
                {
                    player.sendMessage("/wolfpickup | Toggle wolf pickup on/off  (alias: /wp or /wolfp)");
                }
                if (MyWolfList.getMyWolf(player).SkillSystem.hasSkill("Behavior") && MyWolfList.getMyWolf(player).SkillSystem.getSkill("Behavior").getLevel() > 0)
                {
                    player.sendMessage("/wolfbehavior | Toggles the behaivior  (alias: /wb or /wolfb)");
                }
            }
            player.sendMessage("");
            player.sendMessage("For more information read the Command-Page on BukkitDev");
            player.sendMessage("-----------------------------------------------------");
        }
        return true;
    }
}

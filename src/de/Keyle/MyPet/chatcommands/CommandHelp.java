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

import de.Keyle.MyPet.util.MyPetList;
import de.Keyle.MyPet.util.MyPetPermissions;
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
            player.sendMessage("--------------- MyPet - Help -------------------------");
            player.sendMessage("/wolfinfo [player] | Display info about a MyPet  (alias: /winfo)");
            if (MyPetPermissions.has(player, "MyPet.admin"))
            {
                player.sendMessage("/wolfadmin [PlayerName] name/exp [Value] | (alias: /ws or /wolfs)");
            }
            if (MyPetList.hasMyWolf(player))
            {
                player.sendMessage("/wolfname <newwolfname> | Set wolf name");
                player.sendMessage("/wolfrelease <wolfname> | Release your wolf");
                player.sendMessage("/wolfstop | MyPet stopps attacking  (alias: /ws or /wolfs)");
                player.sendMessage("/wolfcall | Call your wolf  (alias: /wc or /wolfc)");
                player.sendMessage("/wolfskill | Shows the skill-levels");

                if (MyPetList.getMyWolf(player).skillSystem.hasSkill("Inventory") && MyPetList.getMyWolf(player).skillSystem.getSkill("Inventory").getLevel() > 0)
                {
                    player.sendMessage("/wolfinventory | Open the inventory of the wolf  (alias: /wi or /wolfi)");
                }
                if (MyPetList.getMyWolf(player).skillSystem.hasSkill("Pickup") && MyPetList.getMyWolf(player).skillSystem.getSkill("Pickup").getLevel() > 0)
                {
                    player.sendMessage("/wolfpickup | Toggle wolf pickup on/off  (alias: /wp or /wolfp)");
                }
                if (MyPetList.getMyWolf(player).skillSystem.hasSkill("Behavior") && MyPetList.getMyWolf(player).skillSystem.getSkill("Behavior").getLevel() > 0)
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
/*
 * Copyright (C) 2011-2013 Keyle
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
            player.sendMessage("/petinfo [player] | Display info about a MyPet  (alias: /pinfo)");
            if (MyPetPermissions.has(player, "MyPet.admin"))
            {
                player.sendMessage("/petadmin [PlayerName] name/exp/respawn [Value]");
            }
            if (MyPetList.hasMyPet(player))
            {
                player.sendMessage("/petname <new pet name> | Set the name of your pet");
                player.sendMessage("/petrelease <petname> | Release your pet");
                player.sendMessage("/petstop | MyPet stopps attacking  (alias: /ps or /pets)");
                player.sendMessage("/petcall | Call your pet  (alias: /pc or /petc)");
                player.sendMessage("/petsendaway | Sends your pet away  (alias: /psa or /petsa)");
                player.sendMessage("/petskill | Shows the skill-levels");
                player.sendMessage("/petchooseskilltree | Shows and chooses skilltrees  (alias: /pcst or /petcst)");

                if (MyPetList.getMyPet(player).getSkills().getSkillLevel("Inventory") > 0)
                {
                    player.sendMessage("/petinventory | Opens the inventory of the pet  (alias: /pi or /peti)");
                }
                if (MyPetList.getMyPet(player).getSkills().getSkillLevel("Pickup") > 0)
                {
                    player.sendMessage("/petpickup | Toggle pickup on/off  (alias: /pp or /petp)");
                }
                if (MyPetList.getMyPet(player).getSkills().getSkillLevel("Behavior") > 0)
                {
                    player.sendMessage("/petbehavior | Toggles the behaivior  (alias: /pb or /petb)");
                }
            }
            player.sendMessage("");
            player.sendMessage("For more info read the command page on: mypet.keyle.de");
            player.sendMessage("-----------------------------------------------------");
        }
        return true;
    }
}
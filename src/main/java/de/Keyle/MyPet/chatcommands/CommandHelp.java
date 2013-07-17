/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.chatcommands;

import de.Keyle.MyPet.entity.types.MyPetList;
import de.Keyle.MyPet.util.MyPetPermissions;
import de.Keyle.MyPet.util.locale.MyPetLocales;
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
            player.sendMessage("--------------- MyPet - " + MyPetLocales.getString("Name.Help", player) + " -------------------------");
            player.sendMessage("/petinfo" + MyPetLocales.getString("Message.Help.PetInfo", player));
            player.sendMessage("/pettype" + MyPetLocales.getString("Message.Help.PetType", player));
            if (MyPetPermissions.has(player, "MyPet.user.capturehelper"))
            {
                player.sendMessage("/petcapturehelper" + MyPetLocales.getString("Message.Help.PetCaptureHelper", player));
            }
            if (MyPetPermissions.has(player, "MyPet.admin", false))
            {
                player.sendMessage("/petadmin" + MyPetLocales.getString("Message.Help.PetAdmin", player));
            }
            if (MyPetList.hasMyPet(player))
            {
                player.sendMessage("/petname" + MyPetLocales.getString("Message.Help.PetName", player));
                player.sendMessage("/petrelease" + MyPetLocales.getString("Message.Help.PetRelease", player));
                player.sendMessage("/petstop" + MyPetLocales.getString("Message.Help.PetStop", player));
                player.sendMessage("/petcall" + MyPetLocales.getString("Message.Help.PetCall", player));
                player.sendMessage("/petsendaway" + MyPetLocales.getString("Message.Help.PetSendAway", player));
                player.sendMessage("/petskill" + MyPetLocales.getString("Message.Help.PetSkill", player));
                player.sendMessage("/petchooseskilltree" + MyPetLocales.getString("Message.Help.PetChooseSkilltree", player));

                if (MyPetList.getMyPet(player).getSkills().isSkillActive("Inventory"))
                {
                    player.sendMessage("/petinventory" + MyPetLocales.getString("Message.Help.PetInventory", player));
                }
                if (MyPetList.getMyPet(player).getSkills().isSkillActive("Beacon"))
                {
                    player.sendMessage("/petbeacon" + MyPetLocales.getString("Message.Help.PetBeacon", player));
                }
                if (MyPetList.getMyPet(player).getSkills().isSkillActive("Pickup"))
                {
                    player.sendMessage("/petpickup" + MyPetLocales.getString("Message.Help.PetPickup", player));
                }
                if (MyPetList.getMyPet(player).getSkills().isSkillActive("Behavior"))
                {
                    player.sendMessage("/petbehavior" + MyPetLocales.getString("Message.Help.PetBehavior", player));
                }
            }
            player.sendMessage("");
            player.sendMessage(MyPetLocales.getString("Message.Help.MoreInfo", player) + " http://mypet.keyle.de");
            player.sendMessage("-----------------------------------------------------");
            return true;
        }
        sender.sendMessage("You can't use this command from server console!");
        return true;
    }
}
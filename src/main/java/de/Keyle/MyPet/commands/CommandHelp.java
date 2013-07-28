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

package de.Keyle.MyPet.commands;

import de.Keyle.MyPet.entity.types.MyPetList;
import de.Keyle.MyPet.util.Permissions;
import de.Keyle.MyPet.util.locale.Locales;
import org.bukkit.ChatColor;
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
            player.sendMessage("-------------------- " + ChatColor.GOLD + "MyPet - " + Locales.getString("Name.Help", player) + ChatColor.RESET + " --------------------");
            player.sendMessage(ChatColor.GOLD + "/petinfo" + ChatColor.RESET + ": " + Locales.getString("Message.Help.PetInfo", player));
            player.sendMessage(ChatColor.GOLD + "/pettype" + ChatColor.RESET + ": " + Locales.getString("Message.Help.PetType", player));
            if (Permissions.has(player, "MyPet.user.capturehelper"))
            {
                player.sendMessage(ChatColor.GOLD + "/petcapturehelper" + ChatColor.RESET + ": " + Locales.getString("Message.Help.PetCaptureHelper", player));
            }
            if (Permissions.has(player, "MyPet.admin", false))
            {
                player.sendMessage(ChatColor.GOLD + "/petadmin" + ChatColor.RESET + ": " + Locales.getString("Message.Help.PetAdmin", player));
            }
            if (MyPetList.hasMyPet(player))
            {
                player.sendMessage(ChatColor.GOLD + "/petname" + ChatColor.RESET + ": " + Locales.getString("Message.Help.PetName", player));
                player.sendMessage(ChatColor.GOLD + "/petrelease" + ChatColor.RESET + ": " + Locales.getString("Message.Help.PetRelease", player));
                player.sendMessage(ChatColor.GOLD + "/petstop" + ChatColor.RESET + ": " + Locales.getString("Message.Help.PetStop", player));
                player.sendMessage(ChatColor.GOLD + "/petcall" + ChatColor.RESET + ": " + Locales.getString("Message.Help.PetCall", player));
                player.sendMessage(ChatColor.GOLD + "/petsendaway" + ChatColor.RESET + ": " + Locales.getString("Message.Help.PetSendAway", player));
                player.sendMessage(ChatColor.GOLD + "/petskill" + ChatColor.RESET + ": " + Locales.getString("Message.Help.PetSkill", player));
                player.sendMessage(ChatColor.GOLD + "/petchooseskilltree" + ChatColor.RESET + ": " + Locales.getString("Message.Help.PetChooseSkilltree", player));

                if (MyPetList.getMyPet(player).getSkills().isSkillActive("Inventory"))
                {
                    player.sendMessage(ChatColor.GOLD + "/petinventory" + ChatColor.RESET + ": " + Locales.getString("Message.Help.PetInventory", player));
                }
                if (MyPetList.getMyPet(player).getSkills().isSkillActive("Beacon"))
                {
                    player.sendMessage(ChatColor.GOLD + "/petbeacon" + ChatColor.RESET + ": " + Locales.getString("Message.Help.PetBeacon", player));
                }
                if (MyPetList.getMyPet(player).getSkills().isSkillActive("Pickup"))
                {
                    player.sendMessage(ChatColor.GOLD + "/petpickup" + ChatColor.RESET + ": " + Locales.getString("Message.Help.PetPickup", player));
                }
                if (MyPetList.getMyPet(player).getSkills().isSkillActive("Behavior"))
                {
                    player.sendMessage(ChatColor.GOLD + "/petbehavior" + ChatColor.RESET + ": " + Locales.getString("Message.Help.PetBehavior", player));
                }
            }
            player.sendMessage("");
            player.sendMessage(Locales.getString("Message.Help.MoreInfo", player) + ChatColor.GOLD + " http://mypet.keyle.de");
            player.sendMessage("----------------------------------------------------");
            return true;
        }
        sender.sendMessage("You can't use this command from server console!");
        return true;
    }
}
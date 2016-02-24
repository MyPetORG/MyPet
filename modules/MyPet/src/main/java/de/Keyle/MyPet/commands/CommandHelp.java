/*
 * This file is part of mypet
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet is licensed under the GNU Lesser General Public License.
 *
 * mypet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.commands;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.skill.skills.Behavior;
import de.Keyle.MyPet.skill.skills.Inventory;
import de.Keyle.MyPet.skill.skills.Pickup;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandHelp implements CommandExecutor, TabCompleter {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            player.sendMessage("-------------------- " + ChatColor.GOLD + "MyPet - " + Translation.getString("Name.Help", player) + ChatColor.RESET + " --------------------");
            player.sendMessage(ChatColor.GOLD + "/petinfo" + ChatColor.RESET + ": " + Translation.getString("Message.Command.Help.Info", player));
            player.sendMessage(ChatColor.GOLD + "/pettype" + ChatColor.RESET + ": " + Translation.getString("Message.Command.Help.Type", player));
            player.sendMessage(ChatColor.GOLD + "/petoptions" + ChatColor.RESET + ": " + Translation.getString("Message.Command.Help.Options", player));
            if (Permissions.has(player, "MyPet.user.command.capturehelper")) {
                player.sendMessage(ChatColor.GOLD + "/petcapturehelper" + ChatColor.RESET + ": " + Translation.getString("Message.Command.Help.CaptureHelper", player));
            }
            if (Permissions.has(player, "MyPet.admin", false)) {
                player.sendMessage(ChatColor.GOLD + "/petadmin" + ChatColor.RESET + ": " + Translation.getString("Message.Command.Help.Admin", player));
            }
            if (MyPetApi.getMyPetList().hasActiveMyPet(player)) {
                if (Permissions.has(player, "MyPet.user.command.name")) {
                    player.sendMessage(ChatColor.GOLD + "/petname" + ChatColor.RESET + ": " + Translation.getString("Message.Command.Help.Name", player));
                }
                if (Permissions.has(player, "MyPet.user.command.release")) {
                    player.sendMessage(ChatColor.GOLD + "/petrelease" + ChatColor.RESET + ": " + Translation.getString("Message.Command.Help.Release", player));
                }
                player.sendMessage(ChatColor.GOLD + "/petstop" + ChatColor.RESET + ": " + Translation.getString("Message.Command.Help.Stop", player));
                player.sendMessage(ChatColor.GOLD + "/petcall" + ChatColor.RESET + ": " + Translation.getString("Message.Command.Help.Call", player));
                player.sendMessage(ChatColor.GOLD + "/petsendaway" + ChatColor.RESET + ": " + Translation.getString("Message.Command.Help.SendAway", player));
                if (Permissions.has(player, "MyPet.user.command.respawn")) {
                    player.sendMessage(ChatColor.GOLD + "/petrespawn" + ChatColor.RESET + ": " + Translation.getString("Message.Command.Help.Respawn", player));
                }
                if (Permissions.has(player, "MyPet.user.command.switch")) {
                    player.sendMessage(ChatColor.GOLD + "/petswitch" + ChatColor.RESET + ": " + Translation.getString("Message.Command.Help.Switch", player));
                }
                if (Permissions.has(player, "MyPet.user.command.trade.offer") || Permissions.has(player, "MyPet.user.command.trade.recieve")) {
                    player.sendMessage(ChatColor.GOLD + "/pettrade" + ChatColor.RESET + ": " + Translation.getString("Message.Command.Help.Trade", player));
                }
                player.sendMessage(ChatColor.GOLD + "/petskill" + ChatColor.RESET + ": " + Translation.getString("Message.Command.Help.Skill", player));
                player.sendMessage(ChatColor.GOLD + "/petchooseskilltree" + ChatColor.RESET + ": " + Translation.getString("Message.Command.Help.ChooseSkilltree", player));

                if (MyPetApi.getMyPetList().getMyPet(player).getSkills().isSkillActive(Inventory.class)) {
                    player.sendMessage(ChatColor.GOLD + "/petinventory" + ChatColor.RESET + ": " + Translation.getString("Message.Command.Help.Inventory", player));
                }
                if (MyPetApi.getMyPetList().getMyPet(player).getSkills().isSkillActive(Pickup.class)) {
                    player.sendMessage(ChatColor.GOLD + "/petpickup" + ChatColor.RESET + ": " + Translation.getString("Message.Command.Help.Pickup", player));
                }
                if (MyPetApi.getMyPetList().getMyPet(player).getSkills().isSkillActive(Behavior.class)) {
                    player.sendMessage(ChatColor.GOLD + "/petbehavior" + ChatColor.RESET + ": " + Translation.getString("Message.Command.Help.Behavior", player));
                }
            }
            player.sendMessage("");
            player.sendMessage(Translation.getString("Message.Command.Help.MoreInfo", player) + ChatColor.GOLD + " " + Configuration.Misc.WIKI_URL);
            player.sendMessage("----------------------------------------------------");
            return true;
        }
        sender.sendMessage("You can't use this command from server console!");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] strings) {
        return CommandAdmin.EMPTY_LIST;
    }
}
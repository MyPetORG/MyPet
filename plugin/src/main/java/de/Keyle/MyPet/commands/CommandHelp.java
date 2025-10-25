/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.commands.CommandTabCompleter;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.skill.skills.BackpackImpl;
import de.Keyle.MyPet.skill.skills.BeaconImpl;
import de.Keyle.MyPet.skill.skills.BehaviorImpl;
import de.Keyle.MyPet.skill.skills.PickupImpl;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandHelp implements CommandTabCompleter {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            player.sendMessage("-------------------- " + ChatColor.GOLD + "MyPet - " + Translation.getString("Name.Help", player) + ChatColor.RESET + " --------------------");
            if (Permissions.has(player, "MyPet.admin", false)) {
                player.sendMessage(Util.formatText(Translation.getString("Message.Command.Help.Admin", player), "/petadmin"));
            }
            player.sendMessage(Util.formatText(Translation.getString("Message.Command.Help.Info", player), "/petinfo"));
            player.sendMessage(Util.formatText(Translation.getString("Message.Command.Help.Shop", player), "/petshop"));
            player.sendMessage(Util.formatText(Translation.getString("Message.Command.Help.Options", player), "/petoptions"));
            if (Permissions.has(player, "MyPet.command.capturehelper")) {
                player.sendMessage(Util.formatText(Translation.getString("Message.Command.Help.CaptureHelper", player), "/petcapturehelper"));
            }
            if (MyPetApi.getMyPetManager().hasActiveMyPet(player)) {
                player.sendMessage(Util.formatText(Translation.getString("Message.Command.Help.Call", player), "/petcall"));
                player.sendMessage(Util.formatText(Translation.getString("Message.Command.Help.SendAway", player), "/petsendaway"));
                player.sendMessage(Util.formatText(Translation.getString("Message.Command.Help.Stop", player), "/petstop"));
                if (Permissions.has(player, "MyPet.command.name")) {
                    player.sendMessage(Util.formatText(Translation.getString("Message.Command.Help.Name", player), "/petname"));
                }
                if (Permissions.has(player, "MyPet.command.release")) {
                    player.sendMessage(Util.formatText(Translation.getString("Message.Command.Help.Release", player), "/petrelease"));
                }
                if (Permissions.has(player, "MyPet.command.respawn")) {
                    player.sendMessage(Util.formatText(Translation.getString("Message.Command.Help.Respawn", player), "/petrespawn"));
                }
                if (Permissions.has(player, "MyPet.command.switch")) {
                    player.sendMessage(Util.formatText(Translation.getString("Message.Command.Help.Switch", player), "/petswitch"));
                    player.sendMessage(Util.formatText(Translation.getString("Message.Command.Help.Store", player), "/petstore"));
                }
                if (Permissions.has(player, "MyPet.command.trade.offer") || Permissions.has(player, "MyPet.command.trade.receive")) {
                    player.sendMessage(Util.formatText(Translation.getString("Message.Command.Help.Trade", player), "/pettrade"));
                }
                player.sendMessage(Util.formatText(Translation.getString("Message.Command.Help.Skill", player), "/petskill"));
                player.sendMessage(Util.formatText(Translation.getString("Message.Command.Help.ChooseSkilltree", player), "/petchooseskilltree"));

                if (MyPetApi.getMyPetManager().getMyPet(player).getSkills().isActive(BackpackImpl.class)) {
                    player.sendMessage(Util.formatText(Translation.getString("Message.Command.Help.Inventory", player), "/petinventory"));
                }
                if (MyPetApi.getMyPetManager().getMyPet(player).getSkills().isActive(PickupImpl.class)) {
                    player.sendMessage(Util.formatText(Translation.getString("Message.Command.Help.Pickup", player), "/petpickup"));
                }
                if (MyPetApi.getMyPetManager().getMyPet(player).getSkills().isActive(BehaviorImpl.class)) {
                    player.sendMessage(Util.formatText(Translation.getString("Message.Command.Help.Behavior", player), "/petbehavior"));
                }
                if (MyPetApi.getMyPetManager().getMyPet(player).getSkills().isActive(BeaconImpl.class)) {
                    player.sendMessage(Util.formatText(Translation.getString("Message.Command.Help.Beacon", player), "/petbeacon"));
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
        return Collections.emptyList();
    }
}
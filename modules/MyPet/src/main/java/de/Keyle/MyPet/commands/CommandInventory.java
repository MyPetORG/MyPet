/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPet.PetState;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.skill.skills.Inventory;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandInventory implements CommandExecutor, TabCompleter {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length == 0) {
                if (MyPetApi.getMyPetList().hasActiveMyPet(player)) {
                    MyPet myPet = MyPetApi.getMyPetList().getMyPet(player);
                    if (myPet.getStatus() == PetState.Despawned) {
                        sender.sendMessage(Util.formatText(Translation.getString("Message.Call.First", player), myPet.getPetName()));
                        return true;
                    }
                    if (myPet.getStatus() == PetState.Dead) {
                        sender.sendMessage(Util.formatText(Translation.getString("Message.Call.Dead", player), myPet.getPetName(), myPet.getRespawnTime()));
                        return true;
                    }
                    if (!Permissions.hasExtended(player, "MyPet.user.extended.Inventory") && !Permissions.has(player, "MyPet.admin", false)) {
                        myPet.getOwner().sendMessage(Translation.getString("Message.No.CanUse", player));
                        return true;
                    }
                    if (myPet.getSkills().hasSkill(Inventory.class)) {
                        myPet.getSkills().getSkill(Inventory.class).activate();
                    }
                } else {
                    sender.sendMessage(Translation.getString("Message.No.HasPet", player));
                }
            } else if (args.length == 1 && Permissions.has(player, "MyPet.admin", false)) {
                Player petOwner = Bukkit.getServer().getPlayer(args[0]);

                if (petOwner == null || !petOwner.isOnline()) {
                    sender.sendMessage(Translation.getString("Message.No.PlayerOnline", player));
                } else if (MyPetApi.getMyPetList().hasActiveMyPet(petOwner)) {
                    MyPet myPet = MyPetApi.getMyPetList().getMyPet(petOwner);
                    if (myPet.getSkills().isSkillActive(Inventory.class)) {
                        myPet.getSkills().getSkill(Inventory.class).openInventory(player);
                    }
                }
            }
            return true;
        }
        sender.sendMessage("You can't use this command from server console!");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if (strings.length == 1 && Permissions.has((Player) commandSender, "MyPet.admin", false)) {
            return null;
        }
        return CommandAdmin.EMPTY_LIST;
    }
}
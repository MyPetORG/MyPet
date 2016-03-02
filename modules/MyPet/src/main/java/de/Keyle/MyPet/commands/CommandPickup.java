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
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPet.PetState;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.skill.skills.Pickup;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandPickup implements CommandExecutor, TabCompleter {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player owner = (Player) sender;
            if (MyPetApi.getMyPetList().hasActiveMyPet(owner)) {
                MyPet myPet = MyPetApi.getMyPetList().getMyPet(owner);

                if (!Permissions.hasExtended(myPet.getOwner().getPlayer(), "MyPet.user.extended.Pickup")) {
                    sender.sendMessage(Translation.getString("Message.No.Allowed", owner));
                    return true;
                } else if (myPet.getStatus() == PetState.Despawned) {
                    sender.sendMessage(Util.formatText(Translation.getString("Message.Call.First", owner), myPet.getPetName()));
                    return true;
                } else if (myPet.getStatus() == PetState.Dead) {
                    sender.sendMessage(Util.formatText(Translation.getString("Message.Call.Dead", owner), myPet.getPetName(), myPet.getRespawnTime()));
                    return true;
                }
                if (myPet.getSkills().hasSkill(Pickup.class)) {
                    myPet.getSkills().getSkill(Pickup.class).activate();
                }
            } else {
                sender.sendMessage(Translation.getString("Message.No.HasPet", owner));
            }
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
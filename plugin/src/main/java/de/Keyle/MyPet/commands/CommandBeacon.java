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
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.commands.CommandTabCompleter;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPet.PetState;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.skill.skills.BeaconImpl;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandBeacon implements CommandTabCompleter {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (WorldGroup.getGroupByWorld(player.getWorld()).isDisabled()) {
                player.sendMessage(Translation.getString("Message.No.AllowedHere", player));
                return true;
            }
            if (MyPetApi.getMyPetManager().hasActiveMyPet(player)) {
                MyPet myPet = MyPetApi.getMyPetManager().getMyPet(player);
                if (!Permissions.hasExtended(player, "MyPet.extended.beacon")) {
                    myPet.getOwner().sendMessage(Translation.getString("Message.No.CanUse", player));
                    return true;
                }
                if (myPet.getStatus() == PetState.Despawned) {
                    sender.sendMessage(Util.formatText(Translation.getString("Message.Call.First", player), myPet.getPetName()));
                    return true;
                }
                if (myPet.getStatus() == PetState.Dead) {
                    sender.sendMessage(Util.formatText(Translation.getString("Message.Action.Dead", player), myPet.getPetName()));
                    return true;
                }
                if (myPet.getSkills().isActive(BeaconImpl.class)) {
                    myPet.getSkills().get(BeaconImpl.class).activate();
                } else {
                    sender.sendMessage(Util.formatText(Translation.getString("Message.No.Skill", player), myPet.getPetName(), Translation.getString("Name.Skill.Beacon", player)));
                }
                return true;
            } else {
                sender.sendMessage(Translation.getString("Message.No.HasPet", player));
            }
            return true;
        }
        sender.sendMessage("You can't use this command from server console!");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        return Collections.emptyList();
    }
}
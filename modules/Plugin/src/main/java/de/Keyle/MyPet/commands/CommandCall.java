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
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.commands.CommandTabCompleter;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CommandCall implements CommandTabCompleter {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player petOwner = (Player) sender;
            if (WorldGroup.getGroupByWorld(petOwner.getWorld()).isDisabled()) {
                sender.sendMessage(Translation.getString("Message.No.AllowedHere", petOwner));
                return true;
            }
            if (MyPetApi.getMyPetManager().hasActiveMyPet(petOwner)) {
                MyPet myPet = MyPetApi.getMyPetManager().getMyPet(petOwner);

                myPet.removePet(true);

                switch (myPet.createEntity()) {
                    case Success:
                        sender.sendMessage(Util.formatText(Translation.getString("Message.Command.Call.Success", petOwner), myPet.getPetName()));
                        break;
                    case Canceled:
                        sender.sendMessage(Util.formatText(Translation.getString("Message.Spawn.Prevent", petOwner), myPet.getPetName()));
                        break;
                    case NoSpace:
                        sender.sendMessage(Util.formatText(Translation.getString("Message.Spawn.NoSpace", petOwner), myPet.getPetName()));
                        break;
                    case NotAllowed:
                        sender.sendMessage(Util.formatText(Translation.getString("Message.No.AllowedHere", petOwner), myPet.getPetName()));
                        break;
                    case Dead:
                        if (Configuration.Respawn.DISABLE_AUTO_RESPAWN) {
                            sender.sendMessage(Util.formatText(Translation.getString("Message.Call.Dead", petOwner), myPet.getPetName()));
                        } else {
                            sender.sendMessage(Util.formatText(Translation.getString("Message.Call.Dead.Respawn", petOwner), myPet.getPetName(), myPet.getRespawnTime()));
                        }
                        break;
                    case Flying:
                        sender.sendMessage(Util.formatText(Translation.getString("Message.Spawn.Flying", petOwner), myPet.getPetName()));
                        break;
                    case Spectator:
                        sender.sendMessage(Util.formatText(Translation.getString("Message.Spawn.Spectator", petOwner), myPet.getPetName()));
                        break;
                }
                return true;
            } else {
                sender.sendMessage(Translation.getString("Message.No.HasPet", petOwner));
            }
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
/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetList;
import de.Keyle.MyPet.util.Util;
import de.Keyle.MyPet.util.locale.Locales;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandCall implements CommandExecutor {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player petOwner = (Player) sender;
            if (MyPetList.hasMyPet(petOwner)) {
                MyPet myPet = MyPetList.getMyPet(petOwner);

                myPet.removePet(true);

                switch (myPet.createPet()) {
                    case Success:
                        sender.sendMessage(Util.formatText(Locales.getString("Message.Command.Call.Success", petOwner), myPet.getPetName()));
                        break;
                    case Canceled:
                        sender.sendMessage(Util.formatText(Locales.getString("Message.Spawn.Prevent", petOwner), myPet.getPetName()));
                        break;
                    case NoSpace:
                        sender.sendMessage(Util.formatText(Locales.getString("Message.Spawn.NoSpace", petOwner), myPet.getPetName()));
                        break;
                    case NotAllowed:
                        sender.sendMessage(Locales.getString("Message.No.AllowedHere", petOwner).replace("%petname%", myPet.getPetName()));
                        break;
                    case Dead:
                        sender.sendMessage(Util.formatText(Locales.getString("Message.Call.Dead", petOwner), myPet.getPetName(), myPet.getRespawnTime()));
                        break;
                }
                return true;
            } else {
                sender.sendMessage(Locales.getString("Message.No.HasPet", petOwner));
            }
            return true;
        }
        sender.sendMessage("You can't use this command from server console!");
        return true;
    }
}
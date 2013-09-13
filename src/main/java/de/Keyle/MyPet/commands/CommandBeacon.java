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

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.entity.types.MyPetList;
import de.Keyle.MyPet.skill.skills.implementation.Beacon;
import de.Keyle.MyPet.util.Permissions;
import de.Keyle.MyPet.util.Util;
import de.Keyle.MyPet.util.locale.Locales;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandBeacon implements CommandExecutor, TabCompleter {
    private static List<String> emptyList = new ArrayList<String>();

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length == 1 && !args[0].equalsIgnoreCase("stop") && Permissions.has(player, "MyPet.admin", false)) {
                Player petOwner = Bukkit.getServer().getPlayer(args[0]);

                if (petOwner == null || !petOwner.isOnline()) {
                    sender.sendMessage(Locales.getString("Message.No.PlayerOnline", player));
                    return true;
                } else if (!MyPetList.hasMyPet(petOwner)) {
                    sender.sendMessage(Util.formatText(Locales.getString("Message.No.UserHavePet", player), petOwner.getName()));
                    return true;
                }

                MyPet myPet = MyPetList.getMyPet(petOwner);
                if (myPet.getSkills().isSkillActive(Beacon.class)) {
                    myPet.getSkills().getSkill(Beacon.class).activate(player);
                }
                return true;
            }
            if (MyPetList.hasMyPet(player)) {
                MyPet myPet = MyPetList.getMyPet(player);
                if (!Permissions.hasExtended(player, "MyPet.user.extended.Beacon", true)) {
                    myPet.sendMessageToOwner(Locales.getString("Message.No.CanUse", player));
                    return true;
                }
                if (myPet.getStatus() == PetState.Despawned) {
                    sender.sendMessage(Util.formatText(Locales.getString("Message.Call.First", player), myPet.getPetName()));
                    return true;
                }
                if (myPet.getStatus() == PetState.Dead) {
                    sender.sendMessage(Util.formatText(Locales.getString("Message.Call.Dead", player), myPet.getPetName(), myPet.getRespawnTime()));
                    return true;
                }
                if (args.length >= 1 && args[0].equalsIgnoreCase("stop")) {
                    myPet.getSkills().getSkill(Beacon.class).stop(true);
                    sender.sendMessage(Locales.getString("Message.Skill.Beacon.Stop", player).replace("%petname%", myPet.getPetName()));
                    return true;
                }
                if (player.getGameMode() == GameMode.CREATIVE && !Permissions.has(player, "MyPet.admin", false)) {
                    sender.sendMessage(Locales.getString("Message.Skill.Beacon.Creative", player).replace("%petname%", myPet.getPetName()));
                    return true;
                }
                if (myPet.getSkills().hasSkill(Beacon.class)) {
                    myPet.getSkills().getSkill(Beacon.class).activate();
                }
            } else {
                sender.sendMessage(Locales.getString("Message.No.HasPet", player));
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
        return emptyList;
    }
}
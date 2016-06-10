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
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.SkillInstance;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandSkill implements CommandExecutor, TabCompleter {
    public static final Map<String, String> icons = new HashMap<>();

    static {
        // http://unicode-table.com/en/#A800
        icons.put("Fire", "\uA800");
        icons.put("Damage", "\uA801");
        icons.put("Behavior", "\uA803");
        icons.put("Beacon", "\uA804");
        icons.put("Wither", "\uA805");
        icons.put("Sprint", "\uA807");
        icons.put("Stomp", "\uA808");
        icons.put("Slow", "\uA809");
        icons.put("Shield", "\uA80A");
        icons.put("Poison", "\uA80C");
        icons.put("Pickup", "\uA80D");
        icons.put("Lightning", "\uA80E");
        icons.put("Knockback", "\uA80F");
        icons.put("Inventory", "\uA810");
        icons.put("Ranged", "\uA811");
        icons.put("HPregeneration", "\uA812");
        icons.put("HP", "\uA813");
        icons.put("Control", "\uA814");
        icons.put("Thorns", "\uA815");
        icons.put("Ride", "\uA816");
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player petOwner = (Player) sender;
            if (args.length > 0 && Permissions.has(petOwner, "MyPet.admin", false)) {
                petOwner = Bukkit.getServer().getPlayer(args[0]);

                if (petOwner == null || !petOwner.isOnline()) {
                    sender.sendMessage(Translation.getString("Message.No.PlayerOnline", petOwner));
                    return true;
                } else if (!MyPetApi.getMyPetManager().hasActiveMyPet(petOwner)) {
                    sender.sendMessage(Util.formatText(Translation.getString("Message.No.UserHavePet", petOwner), petOwner.getName()));
                    return true;
                }
            }

            if (MyPetApi.getMyPetManager().hasActiveMyPet(petOwner)) {
                MyPet myPet = MyPetApi.getMyPetManager().getMyPet(petOwner);
                myPet.autoAssignSkilltree();
                sender.sendMessage(Util.formatText(Translation.getString("Message.Command.Skills.Show", petOwner), myPet.getPetName(), (myPet.getSkilltree() == null ? "-" : myPet.getSkilltree().getDisplayName())));

                for (SkillInstance skill : myPet.getSkills().getSkills()) {
                    if (skill.isActive()) {
                        String message = "  ";
                        if (myPet.getOwner().isUsingResourcePack()) {
                            message += icons.get(skill.getName()) + " ";
                        }
                        sender.sendMessage(message + ChatColor.GREEN + skill.getName(MyPetApi.getPlatformHelper().getPlayerLanguage(petOwner)) + ChatColor.RESET + " " + skill.getFormattedValue());
                    }
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
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if (strings.length == 1 && Permissions.has((Player) commandSender, "MyPet.admin", false)) {
            return null;
        }
        return CommandAdmin.EMPTY_LIST;
    }
}
/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2018 Keyle
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
import de.Keyle.MyPet.api.skill.skills.Behavior.BehaviorMode;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.skill.skills.BehaviorImpl;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandBehavior implements CommandExecutor, TabCompleter {
    private static List<String> behaviorList = new ArrayList<>();

    static {
        for (BehaviorMode mode : BehaviorMode.values()) {
            behaviorList.add(mode.name());
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player petOwner = (Player) sender;
            if (MyPetApi.getMyPetManager().hasActiveMyPet(petOwner)) {
                MyPet myPet = MyPetApi.getMyPetManager().getMyPet(petOwner);

                if (myPet.getStatus() == PetState.Despawned) {
                    sender.sendMessage(Util.formatText(Translation.getString("Message.Call.First", petOwner), myPet.getPetName()));
                    return true;
                }
                if (myPet.getStatus() == PetState.Dead) {
                    sender.sendMessage(Util.formatText(Translation.getString("Message.No.CanUse", petOwner), myPet.getPetName()));
                    return true;
                } else if (myPet.getSkills().has(BehaviorImpl.class)) {
                    BehaviorImpl behaviorSkill = myPet.getSkills().get(BehaviorImpl.class);
                    if (args.length == 1) {
                        if ((args[0].equalsIgnoreCase("friendly") || args[0].equalsIgnoreCase("friend"))) {
                            if (!Permissions.hasExtendedLegacy(petOwner, "MyPet.extended.behavior.", "friendly") || !behaviorSkill.isModeUsable(BehaviorMode.Friendly)) {
                                myPet.getOwner().sendMessage(Translation.getString("Message.No.Allowed", petOwner));
                                return true;
                            }
                            behaviorSkill.setBehavior(BehaviorMode.Friendly);
                        } else if ((args[0].equalsIgnoreCase("aggressive") || args[0].equalsIgnoreCase("Aggro"))) {
                            if (!Permissions.hasExtendedLegacy(petOwner, "MyPet.extended.behavior.", "aggressive") || !behaviorSkill.isModeUsable(BehaviorMode.Aggressive)) {
                                myPet.getOwner().sendMessage(Translation.getString("Message.No.Allowed", petOwner));
                                return true;
                            }
                            behaviorSkill.setBehavior(BehaviorMode.Aggressive);
                        } else if (args[0].equalsIgnoreCase("farm")) {
                            if (!Permissions.hasExtendedLegacy(petOwner, "MyPet.extended.behavior.", "farm") || !behaviorSkill.isModeUsable(BehaviorMode.Farm)) {
                                myPet.getOwner().sendMessage(Translation.getString("Message.No.Allowed", petOwner));
                                return true;
                            }
                            behaviorSkill.setBehavior(BehaviorMode.Farm);
                        } else if (args[0].equalsIgnoreCase("raid")) {
                            if (!Permissions.hasExtendedLegacy(petOwner, "MyPet.extended.behavior.", "raid") || !behaviorSkill.isModeUsable(BehaviorMode.Raid)) {
                                myPet.getOwner().sendMessage(Translation.getString("Message.No.Allowed", petOwner));
                                return true;
                            }
                            behaviorSkill.setBehavior(BehaviorMode.Raid);
                        } else if (args[0].equalsIgnoreCase("duel")) {
                            if (!Permissions.hasExtendedLegacy(petOwner, "MyPet.extended.behavior.", "duel") || !behaviorSkill.isModeUsable(BehaviorMode.Duel)) {
                                myPet.getOwner().sendMessage(Translation.getString("Message.No.Allowed", petOwner));
                                return true;
                            }
                            behaviorSkill.setBehavior(BehaviorMode.Duel);
                        } else if (args[0].equalsIgnoreCase("normal")) {
                            behaviorSkill.setBehavior(BehaviorMode.Normal);
                        } else {
                            behaviorSkill.activate();
                            return false;
                        }
                    } else {
                        behaviorSkill.activate();
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
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] strings) {
        if (sender instanceof Player) {
            if (strings.length == 1) {
                return behaviorList;
            }
        }
        return Collections.emptyList();
    }
}
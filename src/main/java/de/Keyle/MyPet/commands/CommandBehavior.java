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
import de.Keyle.MyPet.entity.types.MyPet.PetState;
import de.Keyle.MyPet.repository.MyPetList;
import de.Keyle.MyPet.skill.skills.implementation.Behavior;
import de.Keyle.MyPet.skill.skills.info.BehaviorInfo.BehaviorState;
import de.Keyle.MyPet.util.Util;
import de.Keyle.MyPet.util.hooks.Permissions;
import de.Keyle.MyPet.util.locale.Locales;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandBehavior implements CommandExecutor, TabCompleter {
    private static List<String> behaviorList = new ArrayList<String>();
    private static List<String> emptyList = new ArrayList<String>();

    static {
        behaviorList.add("normal");
        behaviorList.add("friendly");
        behaviorList.add("aggressive");
        behaviorList.add("raid");
        behaviorList.add("farm");
        behaviorList.add("duel");
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player petOwner = (Player) sender;
            if (MyPetList.hasMyPet(petOwner)) {
                MyPet myPet = MyPetList.getMyPet(petOwner);

                if (myPet.getStatus() == PetState.Despawned) {
                    sender.sendMessage(Util.formatText(Locales.getString("Message.Call.First", petOwner), myPet.getPetName()));
                    return true;
                }
                if (myPet.getStatus() == PetState.Dead) {
                    sender.sendMessage(Util.formatText(Locales.getString("Message.No.CanUse", petOwner), myPet.getPetName()));
                    return true;
                } else if (myPet.getSkills().hasSkill(Behavior.class)) {
                    Behavior behaviorSkill = myPet.getSkills().getSkill(Behavior.class);
                    if (args.length == 1) {
                        if ((args[0].equalsIgnoreCase("friendly") || args[0].equalsIgnoreCase("friend"))) {
                            if (!Permissions.hasExtended(petOwner, "MyPet.user.extended.Behavior.Friendly") || !behaviorSkill.isModeUsable(BehaviorState.Friendly)) {
                                myPet.sendMessageToOwner(Locales.getString("Message.No.Allowed", petOwner));
                                return true;
                            }
                            behaviorSkill.activateBehavior(Behavior.BehaviorState.Friendly);
                        } else if ((args[0].equalsIgnoreCase("aggressive") || args[0].equalsIgnoreCase("Aggro"))) {
                            if (!Permissions.hasExtended(petOwner, "MyPet.user.extended.Behavior.aggressive") || !behaviorSkill.isModeUsable(BehaviorState.Aggressive)) {
                                myPet.sendMessageToOwner(Locales.getString("Message.No.Allowed", petOwner));
                                return true;
                            }
                            behaviorSkill.activateBehavior(Behavior.BehaviorState.Aggressive);
                        } else if (args[0].equalsIgnoreCase("farm")) {
                            if (!Permissions.hasExtended(petOwner, "MyPet.user.extended.Behavior.Farm") || !behaviorSkill.isModeUsable(BehaviorState.Farm)) {
                                myPet.sendMessageToOwner(Locales.getString("Message.No.Allowed", petOwner));
                                return true;
                            }
                            behaviorSkill.activateBehavior(BehaviorState.Farm);
                        } else if (args[0].equalsIgnoreCase("raid")) {
                            if (!Permissions.hasExtended(petOwner, "MyPet.user.extended.Behavior.Raid") || !behaviorSkill.isModeUsable(BehaviorState.Raid)) {
                                myPet.sendMessageToOwner(Locales.getString("Message.No.Allowed", petOwner));
                                return true;
                            }
                            behaviorSkill.activateBehavior(Behavior.BehaviorState.Raid);
                        } else if (args[0].equalsIgnoreCase("duel")) {
                            if (!Permissions.hasExtended(petOwner, "MyPet.user.extended.Behavior.Duel") || !behaviorSkill.isModeUsable(BehaviorState.Duel)) {
                                myPet.sendMessageToOwner(Locales.getString("Message.No.Allowed", petOwner));
                                return true;
                            }
                            behaviorSkill.activateBehavior(Behavior.BehaviorState.Duel);
                        } else if (args[0].equalsIgnoreCase("normal")) {
                            behaviorSkill.activateBehavior(Behavior.BehaviorState.Normal);
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
                sender.sendMessage(Locales.getString("Message.No.HasPet", petOwner));
            }
            return true;
        }
        sender.sendMessage("You can't use this command from server console!");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if (strings.length == 1) {
            return behaviorList;
        }
        return emptyList;
    }
}
/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2016 Keyle
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

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.repository.MyPetList;
import de.Keyle.MyPet.skill.skills.implementation.inventory.ItemStackNBTConverter;
import de.Keyle.MyPet.skill.skilltree.SkillTree;
import de.Keyle.MyPet.skill.skilltree.SkillTreeMobType;
import de.Keyle.MyPet.util.Configuration;
import de.Keyle.MyPet.util.Util;
import de.Keyle.MyPet.util.hooks.Permissions;
import de.Keyle.MyPet.util.iconmenu.IconMenu;
import de.Keyle.MyPet.util.iconmenu.IconMenuItem;
import de.Keyle.MyPet.util.locale.Translation;
import de.keyle.knbt.TagCompound;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandChooseSkilltree implements CommandExecutor, TabCompleter {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("You can't use this command from server console!");
            return true;
        }
        Player player = (Player) sender;
        if (MyPetList.hasActiveMyPet(player)) {
            final MyPet myPet = MyPetList.getMyPet(player);
            final MyPetPlayer myPetOwner = myPet.getOwner();
            if (Configuration.Skilltree.AUTOMATIC_SKILLTREE_ASSIGNMENT && !myPet.getOwner().isMyPetAdmin()) {
                myPet.autoAssignSkilltree();
                sender.sendMessage(Translation.getString("Message.Command.ChoseSkilltree.AutomaticSkilltreeAssignment", myPet.getOwner().getLanguage()));
            } else if (myPet.getSkillTree() != null && Configuration.Skilltree.CHOOSE_SKILLTREE_ONLY_ONCE && !myPet.getOwner().isMyPetAdmin()) {
                sender.sendMessage(Util.formatText(Translation.getString("Message.Command.ChooseSkilltree.OnlyOnce", myPet.getOwner().getLanguage()), myPet.getPetName()));
            } else if (SkillTreeMobType.hasMobType(myPet.getPetType().getTypeName())) {
                SkillTreeMobType skillTreeMobType = SkillTreeMobType.getMobTypeByName(myPet.getPetType().getTypeName());
                if (args.length >= 1) {
                    String skilltreeName = "";
                    for (String arg : args) {
                        skilltreeName += arg + " ";
                    }
                    skilltreeName = skilltreeName.substring(0, skilltreeName.length() - 1);
                    if (skillTreeMobType.hasSkillTree(skilltreeName)) {
                        SkillTree skillTree = skillTreeMobType.getSkillTree(skilltreeName);
                        if (Permissions.has(player, "MyPet.custom.skilltree." + skillTree.getPermission())) {
                            int requiredLevel = skillTree.getRequiredLevel();
                            if (requiredLevel > 1 && myPet.getExperience().getLevel() < requiredLevel) {
                                myPet.sendMessageToOwner(Util.formatText(Translation.getString("Message.Skilltree.RequiresLevel.Message", player), myPet.getPetName(), requiredLevel));
                            } else if (myPet.setSkilltree(skillTree)) {
                                sender.sendMessage(Util.formatText(Translation.getString("Message.Skilltree.SwitchedTo", player), skillTree.getName()));
                                if ((myPet.getOwner().isMyPetAdmin() && Configuration.Skilltree.SWITCH_PENALTY_ADMIN) || !myPet.getOwner().isMyPetAdmin()) {
                                    double switchPenalty = Configuration.Skilltree.SWITCH_PENALTY_FIXED;
                                    switchPenalty += myPet.getExperience().getExp() * Configuration.Skilltree.SWITCH_PENALTY_PERCENT / 100.;

                                    if (requiredLevel > 1) {
                                        double minExp = myPet.getExperience().getExpByLevel(requiredLevel);
                                        switchPenalty = myPet.getExp() - switchPenalty < minExp ? myPet.getExp() - minExp : switchPenalty;
                                    }
                                    myPet.getExperience().removeExp(switchPenalty);
                                }
                            } else {
                                sender.sendMessage(Translation.getString("Message.Skilltree.NotSwitched", player));
                            }
                        } else {
                            sender.sendMessage(Util.formatText(Translation.getString("Message.Command.Skilltree.CantFindSkilltree", player), skilltreeName));
                        }
                    } else {
                        sender.sendMessage(Util.formatText(Translation.getString("Message.Command.Skilltree.CantFindSkilltree", player), skilltreeName));
                    }
                } else {
                    List<SkillTree> availableSkilltrees = new ArrayList<>();
                    for (SkillTree skillTree : skillTreeMobType.getSkillTrees()) {
                        if (Permissions.has(player, "MyPet.custom.skilltree." + skillTree.getPermission())) {
                            availableSkilltrees.add(skillTree);
                        }
                    }

                    if (availableSkilltrees.size() == 0) {
                        sender.sendMessage(Translation.getString("Message.No.CanUse", player));
                        return true;
                    }

                    final Map<Integer, SkillTree> skilltreeSlotMap = new HashMap<>();
                    IconMenu menu = new IconMenu(Util.cutString(Util.formatText(Translation.getString("Message.Skilltree.Available", myPetOwner), myPet.getPetName()), 32), (int) (Math.ceil(availableSkilltrees.size() / 9.) * 9), new IconMenu.OptionClickEventHandler() {
                        @Override
                        public void onOptionClick(IconMenu.OptionClickEvent event) {
                            if (myPet != myPetOwner.getMyPet()) {
                                event.setWillClose(true);
                                event.setWillDestroy(true);
                                return;
                            }
                            if (skilltreeSlotMap.containsKey(event.getPosition())) {
                                SkillTree selecedSkilltree = skilltreeSlotMap.get(event.getPosition());
                                if (selecedSkilltree != null) {
                                    int requiredLevel = selecedSkilltree.getRequiredLevel();
                                    if (requiredLevel > 1 && myPet.getExperience().getLevel() < requiredLevel) {
                                        myPet.sendMessageToOwner(Util.formatText(Translation.getString("Message.Skilltree.RequiresLevel.Message", myPetOwner), myPet.getPetName(), requiredLevel));
                                    } else if (myPet.setSkilltree(selecedSkilltree)) {
                                        myPet.sendMessageToOwner(Util.formatText(Translation.getString("Message.Skilltree.SwitchedTo", myPetOwner), selecedSkilltree.getName()));
                                        if ((myPet.getOwner().isMyPetAdmin() && Configuration.Skilltree.SWITCH_PENALTY_ADMIN) || !myPet.getOwner().isMyPetAdmin()) {
                                            double switchPenalty = Configuration.Skilltree.SWITCH_PENALTY_FIXED;
                                            switchPenalty += myPet.getExperience().getExp() * Configuration.Skilltree.SWITCH_PENALTY_PERCENT / 100.;

                                            if (requiredLevel > 1) {
                                                double minExp = myPet.getExperience().getExpByLevel(requiredLevel);
                                                switchPenalty = myPet.getExp() - switchPenalty < minExp ? myPet.getExp() - minExp : switchPenalty;
                                            }
                                            myPet.getExperience().removeExp(switchPenalty);
                                        }
                                    } else {
                                        myPet.sendMessageToOwner(Translation.getString("Message.Skilltree.NotSwitched", myPetOwner));
                                    }
                                }
                            }
                            event.setWillClose(true);
                            event.setWillDestroy(true);
                        }
                    }, MyPetPlugin.getPlugin());

                    for (int i = 0; i < availableSkilltrees.size(); i++) {
                        SkillTree addedSkilltree = availableSkilltrees.get(i);

                        TagCompound tag = addedSkilltree.getIconItem();
                        net.minecraft.server.v1_8_R3.ItemStack is = ItemStackNBTConverter.compundToItemStack(tag);
                        IconMenuItem option = IconMenuItem.fromNmsItemStack(is);
                        option.setTitle(ChatColor.RESET + "❱❱❱  " + ChatColor.DARK_GREEN + addedSkilltree.getDisplayName() + ChatColor.RESET + "  ❰❰❰");

                        boolean selectable = false;
                        int requiredLevel = addedSkilltree.getRequiredLevel();
                        if (requiredLevel > 1) {
                            selectable = myPet.getExperience().getLevel() >= addedSkilltree.getRequiredLevel();
                        }

                        List<String> description = new ArrayList<>();
                        if (requiredLevel > 1) {
                            String reqLevelMessage = ChatColor.RESET + "▶▶▶  ";
                            if (selectable) {
                                reqLevelMessage += ChatColor.GREEN;
                            } else {
                                reqLevelMessage += ChatColor.DARK_RED;
                            }
                            reqLevelMessage += Util.formatText(Translation.getString("Message.Skilltree.RequiresLevel.Item", myPetOwner), requiredLevel) + ChatColor.RESET + "  ◀◀◀";
                            description.add(reqLevelMessage);
                        }
                        description.addAll(addedSkilltree.getDescription());

                        option.addLore(description);
                        menu.setOption(i, option);
                        skilltreeSlotMap.put(i, addedSkilltree);
                    }
                    menu.open(player);
                }
            }
        } else {
            sender.sendMessage(Translation.getString("Message.No.HasPet", player));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        Player player = (Player) commandSender;
        if (MyPetList.hasActiveMyPet(player)) {
            MyPet myPet = MyPetList.getMyPet(player);
            if (Configuration.Skilltree.AUTOMATIC_SKILLTREE_ASSIGNMENT && !myPet.getOwner().isMyPetAdmin()) {
                return CommandAdmin.EMPTY_LIST;
            } else if (myPet.getSkillTree() != null && Configuration.Skilltree.CHOOSE_SKILLTREE_ONLY_ONCE && !myPet.getOwner().isMyPetAdmin()) {
                return CommandAdmin.EMPTY_LIST;
            } else if (SkillTreeMobType.hasMobType(myPet.getPetType().getTypeName())) {
                SkillTreeMobType skillTreeMobType = SkillTreeMobType.getMobTypeByName(myPet.getPetType().getTypeName());

                List<String> skilltreeList = new ArrayList<>();
                for (SkillTree skillTree : skillTreeMobType.getSkillTrees()) {
                    if (Permissions.has(player, "MyPet.custom.skilltree." + skillTree.getPermission())) {
                        skilltreeList.add(skillTree.getName());
                    }
                }
                return skilltreeList;
            }
        }
        return CommandAdmin.EMPTY_LIST;
    }
}
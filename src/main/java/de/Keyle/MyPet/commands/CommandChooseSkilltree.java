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

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetList;
import de.Keyle.MyPet.skill.skills.implementation.inventory.ItemStackNBTConverter;
import de.Keyle.MyPet.skill.skilltree.SkillTree;
import de.Keyle.MyPet.skill.skilltree.SkillTreeMobType;
import de.Keyle.MyPet.util.*;
import de.Keyle.MyPet.util.locale.Locales;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.craftbukkit.v1_6_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.spout.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandChooseSkilltree implements CommandExecutor, TabCompleter {
    private static List<String> emptyList = new ArrayList<String>();

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("You can't use this command from server console!");
            return true;
        }
        Player player = (Player) sender;
        if (MyPetList.hasMyPet(player)) {
            final MyPet myPet = MyPetList.getMyPet(player);
            final MyPetPlayer myPetOwner = myPet.getOwner();
            if (Configuration.AUTOMATIC_SKILLTREE_ASSIGNMENT && !myPet.getOwner().isMyPetAdmin()) {
                myPet.autoAssignSkilltree();
                sender.sendMessage(Locales.getString("Message.Command.ChoseSkilltree.AutomaticSkilltreeAssignment", myPet.getOwner().getLanguage()));
            } else if (myPet.getSkillTree() != null && Configuration.CHOOSE_SKILLTREE_ONLY_ONCE && !myPet.getOwner().isMyPetAdmin()) {
                sender.sendMessage(Util.formatText(Locales.getString("Message.Command.ChooseSkilltree.OnlyOnce", myPet.getOwner().getLanguage()), myPet.getPetName()));
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
                                myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Skilltree.RequiresLevel.Message", player), myPet.getPetName(), requiredLevel));
                            } else if (myPet.setSkilltree(skillTree)) {
                                sender.sendMessage(Util.formatText(Locales.getString("Message.Skilltree.SwitchedTo", player), skillTree.getName()));
                                if (myPet.getOwner().isMyPetAdmin() && Configuration.SKILLTREE_SWITCH_PENALTY_ADMIN) {
                                    myPet.getExperience().removeExp(Configuration.SKILLTREE_SWITCH_PENALTY_FIXED);
                                    myPet.getExperience().removeExp(myPet.getExperience().getExp() * Configuration.SKILLTREE_SWITCH_PENALTY_PERCENT / 100.);
                                } else {
                                    myPet.getExperience().removeExp(Configuration.SKILLTREE_SWITCH_PENALTY_FIXED);
                                    myPet.getExperience().removeExp(myPet.getExperience().getExp() * Configuration.SKILLTREE_SWITCH_PENALTY_PERCENT / 100.);
                                }
                            } else {
                                sender.sendMessage(Locales.getString("Message.Skilltree.NotSwitched", player));
                            }
                        } else {
                            sender.sendMessage(Util.formatText(Locales.getString("Message.Command.Skilltree.CantFindSkilltree", player), skilltreeName));
                        }
                    } else {
                        sender.sendMessage(Util.formatText(Locales.getString("Message.Command.Skilltree.CantFindSkilltree", player), skilltreeName));
                    }
                } else {
                    List<SkillTree> availableSkilltrees = new ArrayList<SkillTree>();
                    for (SkillTree skillTree : skillTreeMobType.getSkillTrees()) {
                        if (Permissions.has(player, "MyPet.custom.skilltree." + skillTree.getPermission())) {
                            availableSkilltrees.add(skillTree);
                        }
                    }

                    if (availableSkilltrees.size() == 0) {
                        sender.sendMessage(Locales.getString("Message.No.CanUse", player));
                        return true;
                    }

                    final Map<Integer, SkillTree> skilltreeSlotMap = new HashMap<Integer, SkillTree>();
                    IconMenu menu = new IconMenu(Util.formatText(Locales.getString("Message.Skilltree.Available", myPetOwner), myPet.getPetName()), (int) (Math.ceil(availableSkilltrees.size() / 9.) * 9), new IconMenu.OptionClickEventHandler() {
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
                                        myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Skilltree.RequiresLevel.Message", myPetOwner), myPet.getPetName(), requiredLevel));
                                    } else if (myPet.setSkilltree(selecedSkilltree)) {
                                        myPet.sendMessageToOwner(Util.formatText(Locales.getString("Message.Skilltree.SwitchedTo", myPetOwner), selecedSkilltree.getName()));
                                        if (myPet.getOwner().isMyPetAdmin() && Configuration.SKILLTREE_SWITCH_PENALTY_ADMIN) {
                                            myPet.getExperience().removeExp(Configuration.SKILLTREE_SWITCH_PENALTY_FIXED);
                                            myPet.getExperience().removeExp(myPet.getExperience().getExp() * Configuration.SKILLTREE_SWITCH_PENALTY_PERCENT / 100.);
                                        } else {
                                            myPet.getExperience().removeExp(Configuration.SKILLTREE_SWITCH_PENALTY_FIXED);
                                            myPet.getExperience().removeExp(myPet.getExperience().getExp() * Configuration.SKILLTREE_SWITCH_PENALTY_PERCENT / 100.);
                                        }
                                    } else {
                                        myPet.sendMessageToOwner(Locales.getString("Message.Skilltree.NotSwitched", myPetOwner));
                                    }
                                }
                            }
                            event.setWillClose(true);
                            event.setWillDestroy(true);
                        }
                    }, MyPetPlugin.getPlugin());

                    for (int i = 0; i < availableSkilltrees.size(); i++) {
                        SkillTree addedSkilltree = availableSkilltrees.get(i);

                        CompoundTag tag = addedSkilltree.getIconItem();
                        net.minecraft.server.v1_6_R3.ItemStack is = ItemStackNBTConverter.CompundToItemStack(tag);
                        ItemStack shownItem = CraftItemStack.asCraftMirror(is);


                        boolean selectable = false;
                        int requiredLevel = addedSkilltree.getRequiredLevel();
                        if (requiredLevel > 1) {
                            selectable = myPet.getExperience().getLevel() >= addedSkilltree.getRequiredLevel();
                        }

                        int requireOffset = requiredLevel > 1 ? 1 : 0;
                        String[] descriptionArray = new String[addedSkilltree.getDescription().size() + requireOffset];
                        if (requireOffset == 1) {
                            descriptionArray[0] = ChatColor.RESET + "▶▶▶  ";
                            if (selectable) {
                                descriptionArray[0] += ChatColor.GREEN;
                            } else {
                                descriptionArray[0] += ChatColor.DARK_RED;
                            }
                            descriptionArray[0] += Util.formatText(Locales.getString("Message.Skilltree.RequiresLevel.Item", myPetOwner), requiredLevel) + ChatColor.RESET + "  ◀◀◀";
                        }

                        for (int j = 0; j < addedSkilltree.getDescription().size(); j++) {
                            descriptionArray[j + requireOffset] = ChatColor.RESET + Colorizer.setColors(String.valueOf(addedSkilltree.getDescription().get(j)));
                        }

                        menu.setOption(i, shownItem, ChatColor.RESET + "❱❱❱  " + ChatColor.DARK_GREEN + addedSkilltree.getDisplayName() + ChatColor.RESET + "  ❰❰❰", descriptionArray);
                        skilltreeSlotMap.put(i, addedSkilltree);
                    }
                    menu.open(player);
                }
            }
        } else {
            sender.sendMessage(Locales.getString("Message.No.HasPet", player));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        Player player = (Player) commandSender;
        if (MyPetList.hasMyPet(player)) {
            MyPet myPet = MyPetList.getMyPet(player);
            if (Configuration.AUTOMATIC_SKILLTREE_ASSIGNMENT && !myPet.getOwner().isMyPetAdmin()) {
                return emptyList;
            } else if (myPet.getSkillTree() != null && Configuration.CHOOSE_SKILLTREE_ONLY_ONCE && !myPet.getOwner().isMyPetAdmin()) {
                return emptyList;
            } else if (SkillTreeMobType.hasMobType(myPet.getPetType().getTypeName())) {
                SkillTreeMobType skillTreeMobType = SkillTreeMobType.getMobTypeByName(myPet.getPetType().getTypeName());

                List<String> skilltreeList = new ArrayList<String>();
                for (SkillTree skillTree : skillTreeMobType.getSkillTrees()) {
                    if (Permissions.has(player, "MyPet.custom.skilltree." + skillTree.getPermission())) {
                        skilltreeList.add(skillTree.getName());
                    }
                }
                return skilltreeList;
            }
        }
        return emptyList;
    }
}
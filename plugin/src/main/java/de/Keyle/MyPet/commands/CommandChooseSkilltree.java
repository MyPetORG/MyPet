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
import de.Keyle.MyPet.api.commands.CommandCategory;
import de.Keyle.MyPet.api.commands.CommandTabCompleter;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.event.MyPetSelectSkilltreeEvent;
import de.Keyle.MyPet.api.gui.IconMenu;
import de.Keyle.MyPet.api.gui.IconMenuItem;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.skill.skilltree.SkilltreeIcon;
import de.Keyle.MyPet.api.util.inventory.material.ItemDatabase;

import de.Keyle.MyPet.api.util.locale.Translation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class CommandChooseSkilltree implements CommandTabCompleter {

    public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("You can't use this command from server console!");
            return true;
        }
        if (WorldGroup.getGroupByWorld(player.getWorld()).isDisabled()) {
            player.sendMessage(Translation.getComponent("Message.No.AllowedHere", player));
            return true;
        }
        if (MyPetApi.getMyPetManager().hasActiveMyPet(player)) {
            final MyPet myPet = MyPetApi.getMyPetManager().getMyPet(player);
            final MyPetPlayer myPetOwner = myPet.getOwner();
            if (Configuration.Skilltree.AUTOMATIC_SKILLTREE_ASSIGNMENT && !myPet.getOwner().isMyPetAdmin()) {
                myPet.autoAssignSkilltree();
                sender.sendMessage(Translation.getComponent("Message.Command.ChooseSkilltree.AutomaticSkilltreeAssignment", myPet.getOwner()));
            } else {
                if (args.length >= 1) {
                    if (Configuration.Skilltree.AUTOMATIC_SKILLTREE_ASSIGNMENT && !myPet.getOwner().isMyPetAdmin()) {
                        myPet.autoAssignSkilltree();
                        sender.sendMessage(Translation.getComponent("Message.Command.ChooseSkilltree.AutomaticSkilltreeAssignment", myPet.getOwner()));
                    } else if (myPet.getSkilltree() != null && Configuration.Skilltree.CHOOSE_SKILLTREE_ONLY_ONCE && !myPet.getOwner().isMyPetAdmin()) {
                        sender.sendMessage(Translation.getFormattedComponent("Message.Command.ChooseSkilltree.OnlyOnce", myPet.getOwner(), myPet.getDisplayName()));
                    } else {
                        StringBuilder skilltreeName = new StringBuilder();
                        for (String arg : args) {
                            skilltreeName.append(arg).append(" ");
                        }
                        skilltreeName = new StringBuilder(skilltreeName.substring(0, skilltreeName.length() - 1));
                        if (MyPetApi.getSkilltreeManager().hasSkilltree(skilltreeName.toString())) {
                            Skilltree skilltree = MyPetApi.getSkilltreeManager().getSkilltree(skilltreeName.toString());
                            if (skilltree.getMobTypes().contains(myPet.getPetType()) && skilltree.checkRequirements(myPet)) {
                                int requiredLevel = skilltree.getRequiredLevel();
                                int maxLevel = skilltree.getMaxLevel();
                                if (requiredLevel > 1 && myPet.getExperience().getLevel() < requiredLevel) {
                                    myPet.getOwner().sendMessage(Translation.getFormattedComponent("Message.Skilltree.RequiresLevel.Message", player, myPet.getDisplayName(), requiredLevel));
                                } else if (myPet.getExperience().getLevel() > maxLevel) {
                                    myPet.getOwner().sendMessage(Translation.getFormattedComponent("Message.Skilltree.MaxLevel.Message", player, myPet.getDisplayName(), maxLevel));
                                } else if (myPet.setSkilltree(skilltree, MyPetSelectSkilltreeEvent.Source.PlayerCommand)) {
                                    sender.sendMessage(Translation.getFormattedComponent("Message.Skilltree.SwitchedTo", player, Util.SANITIZED_MINIMESSAGE.deserialize(skilltree.getDisplayName())));
                                    if (!myPet.getOwner().isMyPetAdmin() || Configuration.Skilltree.SWITCH_FEE_ADMIN) {
                                        double switchPenalty = Configuration.Skilltree.SWITCH_FEE_FIXED;
                                        switchPenalty += myPet.getExperience().getExp() * Configuration.Skilltree.SWITCH_FEE_PERCENT / 100.;

                                        if (requiredLevel > 1) {
                                            double minExp = myPet.getExperience().getExpByLevel(requiredLevel);
                                            switchPenalty = myPet.getExp() - switchPenalty < minExp ? myPet.getExp() - minExp : switchPenalty;
                                        }
                                        if (Configuration.LevelSystem.Experience.ALLOW_LEVEL_DOWNGRADE) {
                                            myPet.getExperience().removeExp(switchPenalty);
                                        } else {
                                            myPet.getExperience().removeCurrentExp(switchPenalty);
                                        }
                                    }
                                } else {
                                    sender.sendMessage(Translation.getComponent("Message.Skilltree.NotSwitched", player));
                                }
                            } else {
                                sender.sendMessage(Translation.getFormattedComponent("Message.Command.Skilltree.CantFindSkilltree", player, skilltreeName.toString()));
                            }
                        } else {
                            sender.sendMessage(Translation.getFormattedComponent("Message.Command.Skilltree.CantFindSkilltree", player, skilltreeName.toString()));
                        }
                    }
                } else {
                    List<Skilltree> availableSkilltrees = new ArrayList<>();
                    for (Skilltree skilltree : MyPetApi.getSkilltreeManager().getOrderedSkilltrees()) {
                        if (skilltree.getMobTypes().contains(myPet.getPetType()) && skilltree.checkRequirements(myPet)) {
                            availableSkilltrees.add(skilltree);
                        }
                    }

                    if (availableSkilltrees.isEmpty()) {
                        sender.sendMessage(Translation.getFormattedComponent("Message.Command.ChooseSkilltree.NoneAvailable", player, myPet.getDisplayName()));
                        return true;
                    }

                    final Map<Integer, Skilltree> skilltreeSlotMap = new HashMap<>();
                    IconMenu menu = new IconMenu(Translation.getFormattedComponent("Message.Skilltree.Available", myPetOwner, myPet.getDisplayName()), event -> {
                        if (myPet != myPetOwner.getMyPet()) {
                            event.setWillClose(true);
                            event.setWillDestroy(true);
                            return;
                        }
                        if (myPet.getSkilltree() != null && Configuration.Skilltree.CHOOSE_SKILLTREE_ONLY_ONCE && !myPet.getOwner().isMyPetAdmin()) {
                            sender.sendMessage(Translation.getFormattedComponent("Message.Command.ChooseSkilltree.OnlyOnce", myPet.getOwner(), myPet.getDisplayName()));
                        } else if (skilltreeSlotMap.containsKey(event.getPosition())) {
                            Skilltree selectedSkilltree = skilltreeSlotMap.get(event.getPosition());
                            if (selectedSkilltree != null) {
                                int requiredLevel = selectedSkilltree.getRequiredLevel();
                                int maxLevel = selectedSkilltree.getMaxLevel();
                                if (requiredLevel > 1 && myPet.getExperience().getLevel() < requiredLevel) {
                                    myPet.getOwner().sendMessage(Translation.getFormattedComponent("Message.Skilltree.RequiresLevel.Message", myPetOwner, myPet.getDisplayName(), requiredLevel));
                                } else if (myPet.getExperience().getLevel() > maxLevel) {
                                    myPet.getOwner().sendMessage(Translation.getFormattedComponent("Message.Skilltree.MaxLevel.Message", myPetOwner, myPet.getDisplayName(), maxLevel));
                                } else if (myPet.setSkilltree(selectedSkilltree, MyPetSelectSkilltreeEvent.Source.PlayerCommand)) {
                                    myPet.getOwner().sendMessage(Translation.getFormattedComponent("Message.Skilltree.SwitchedTo", myPetOwner, Util.SANITIZED_MINIMESSAGE.deserialize(selectedSkilltree.getDisplayName())));
                                    if (!myPet.getOwner().isMyPetAdmin() || Configuration.Skilltree.SWITCH_FEE_ADMIN) {
                                        double switchPenalty = Configuration.Skilltree.SWITCH_FEE_FIXED;
                                        switchPenalty += myPet.getExperience().getExp() * Configuration.Skilltree.SWITCH_FEE_PERCENT / 100.;

                                        if (requiredLevel > 1) {
                                            double minExp = myPet.getExperience().getExpByLevel(requiredLevel);
                                            switchPenalty = myPet.getExp() - switchPenalty < minExp ? myPet.getExp() - minExp : switchPenalty;
                                        }
                                        if (Configuration.LevelSystem.Experience.ALLOW_LEVEL_DOWNGRADE) {
                                            myPet.getExperience().removeExp(switchPenalty);
                                        } else {
                                            myPet.getExperience().removeCurrentExp(switchPenalty);
                                        }
                                    }
                                } else {
                                    myPet.getOwner().sendMessage(Translation.getComponent("Message.Skilltree.NotSwitched", myPetOwner));
                                }
                            }
                        }
                        event.setWillClose(true);
                        event.setWillDestroy(true);
                    }, MyPetApi.getPlugin()).setPaginationIdentifier("ChooseSkilltree");

                    ItemDatabase itemDatabase = MyPetApi.getServiceManager().getService(ItemDatabase.class).get();
                    for (int i = 0; i < availableSkilltrees.size(); i++) {
                        Skilltree addedSkilltree = availableSkilltrees.get(i);

                        SkilltreeIcon icon = addedSkilltree.getIcon();
                        Material material = itemDatabase.getByID(icon.getMaterial());
                        if (material == null) {
                            material = itemDatabase.getByID("oak_sapling");
                        }
                        IconMenuItem option = new IconMenuItem()
                                .setMaterial(material)
                                .setGlowing(icon.isGlowing())
                                .setTitle(Component.text()
                                        .append(Component.text("❱❱❱  "))
                                        .append(Util.SANITIZED_MINIMESSAGE.deserialize(addedSkilltree.getDisplayName()).color(NamedTextColor.DARK_GREEN))
                                        .append(Component.text("  ❰❰❰"))
                                        .build());

                        boolean selectable = false;
                        int requiredLevel = addedSkilltree.getRequiredLevel();
                        if (requiredLevel > 1) {
                            selectable = myPet.getExperience().getLevel() >= addedSkilltree.getRequiredLevel();
                        }

                        if (requiredLevel > 1) {
                            NamedTextColor levelColor = selectable ? NamedTextColor.GREEN : NamedTextColor.DARK_RED;
                            option.addLoreLine(Component.text()
                                    .append(Component.text("▶▶▶  "))
                                    .append(Translation.getFormattedComponent("Message.Skilltree.RequiresLevel.Item", myPetOwner.getLanguage(), requiredLevel).color(levelColor))
                                    .append(Component.text("  ◀◀◀"))
                                    .build());
                        }
                        for (String line : addedSkilltree.getDescription()) {
                            option.addLoreLine(Util.SANITIZED_MINIMESSAGE.deserialize(line));
                        }
                        menu.setOption(i, option);
                        skilltreeSlotMap.put(i, addedSkilltree);
                    }
                    menu.open(player);
                }
            }
        } else {
            sender.sendMessage(Translation.getComponent("Message.No.HasPet", player));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] strings) {
        if (sender instanceof Player player) {
            if (Permissions.has(player, "MyPet.admin", false)) {
                if (MyPetApi.getMyPetManager().hasActiveMyPet(player)) {
                    MyPet myPet = MyPetApi.getMyPetManager().getMyPet(player);
                    List<String> skilltreeList = new ArrayList<>();
                    for (Skilltree skilltree : MyPetApi.getSkilltreeManager().getOrderedSkilltrees()) {
                        if (skilltree.getMobTypes().contains(myPet.getPetType()) && skilltree.checkRequirements(myPet)) {
                            skilltreeList.add(skilltree.getName());
                        }
                    }
                    return filterTabCompletionResults(skilltreeList, strings[0]);
                }
            }
        }
        return Collections.emptyList();
    }

    @Override
    public String getHelpTranslationKey() {
        return "Message.Command.Help.ChooseSkilltree";
    }

    @Override
    public String getHelpCommand() {
        return "/petchooseskilltree";
    }

    @Override
    public boolean isVisibleTo(Player player) {
        return MyPetApi.getMyPetManager().hasActiveMyPet(player);
    }

    @Override
    public int getHelpOrder() {
        return 160;
    }

    @Override
    public CommandCategory getHelpCategory() {
        return CommandCategory.SKILLS;
    }
}
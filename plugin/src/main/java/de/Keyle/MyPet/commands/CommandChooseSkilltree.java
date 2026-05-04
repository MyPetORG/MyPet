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

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.event.PetSelectSkilltreeEvent;
import de.Keyle.MyPet.commands.help.CommandCategory;
import de.Keyle.MyPet.commands.help.HelpEntry;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.gui.IconMenu;
import de.Keyle.MyPet.api.gui.IconMenuItem;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.skill.skilltree.SkilltreeIcon;
import de.Keyle.MyPet.api.util.locale.Locale;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Handles the {@code /petchooseskilltree} command, which allows a player to select a
 * skilltree for their active pet. Skilltrees define the progression path and available
 * skills for a pet as it levels up.
 *
 * <p>When executed without arguments, opens an interactive GUI (icon menu) listing all
 * available skilltrees the pet qualifies for. When a skilltree name is provided as an
 * argument, assigns that skilltree directly without opening the GUI.</p>
 *
 * <p>Skilltree selection may be restricted by configuration:
 * <ul>
 *   <li>{@link Configuration.Skilltree#AUTOMATIC_SKILLTREE_ASSIGNMENT} — when enabled,
 *       skilltrees are auto-assigned and manual selection is blocked (unless admin)</li>
 *   <li>{@link Configuration.Skilltree#CHOOSE_SKILLTREE_ONLY_ONCE} — when enabled,
 *       players cannot change their pet's skilltree after the initial selection (unless admin)</li>
 * </ul>
 *
 * <p>Switching skilltrees may incur an experience penalty based on
 * {@link Configuration.Skilltree#SWITCH_FEE_FIXED} and
 * {@link Configuration.Skilltree#SWITCH_FEE_PERCENT}.</p>
 *
 * <p><b>Usage:</b> {@code /petchooseskilltree [skilltree]}</p>
 * <p><b>Aliases:</b> {@code /pcst}, {@code /petcst}</p>
 * <p><b>Help category:</b> {@link CommandCategory#SKILLS SKILLS} (priority 160)</p>
 *
 * @see Skilltree
 * @see PetSelectSkilltreeEvent
 */
@SuppressWarnings("UnstableApiUsage")
public class CommandChooseSkilltree {

    /**
     * Registers the {@code /petchooseskilltree} Brigadier command and its help entry.
     *
     * <p>The command tree consists of a base literal node (player-only) that opens the
     * skilltree selection GUI, plus an optional greedy-string {@code skilltree} argument
     * with tab-completion of skilltrees available for the player's current pet type.</p>
     *
     * @param commands     the Paper {@link Commands} registrar used to register the Brigadier command
     * @param helpRegistry the {@link HelpRegistry} to register the command's help entry with
     */
    public void register(Commands commands, HelpRegistry helpRegistry) {
        commands.register(
                Commands.literal("petchooseskilltree")
                        .requires(ctx -> ctx.getSender() instanceof Player)
                        .executes(ctx -> {
                            executeGui((Player) ctx.getSource().getSender());
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument("skilltree", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> {
                                    if (ctx.getSource().getSender() instanceof Player player) {
                                        if (MyPetApi.getMyPetManager().hasActiveMyPet(player)) {
                                            MyPet myPet = MyPetApi.getMyPetManager().getMyPet(player);
                                            for (Skilltree skilltree : MyPetApi.getSkilltreeManager().getOrderedSkilltrees()) {
                                                if (skilltree.getMobTypes().contains(myPet.getPetType()) && skilltree.checkRequirements(myPet)) {
                                                    builder.suggest(skilltree.getName());
                                                }
                                            }
                                        }
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    executeSkilltree((Player) ctx.getSource().getSender(), StringArgumentType.getString(ctx, "skilltree"));
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .build(),
                "Choose a skilltree for your pet",
                List.of("pcst", "petcst")
        );

        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.ChooseSkilltree",
                "/petchooseskilltree",
                CommandCategory.SKILLS,
                160,
                player -> MyPetApi.getMyPetManager().hasActiveMyPet(player)
        ));
    }

    /**
     * Opens the skilltree selection GUI for the given player.
     *
     * <p>Builds an {@link IconMenu} displaying all skilltrees compatible with the pet's
     * type and meeting the pet's requirements (e.g., level). Each entry shows the
     * skilltree's icon, display name, required level (color-coded), and description.
     * Clicking an entry applies the selected skilltree.</p>
     *
     * <p>If automatic skilltree assignment is enabled and the player is not an admin,
     * the pet is auto-assigned a skilltree and the GUI is not shown.</p>
     *
     * @param player the player whose pet should receive a skilltree selection GUI
     */
    private void executeGui(Player player) {
        if (WorldGroup.getGroupByWorld(player.getWorld()).isDisabled()) {
            player.sendMessage(Locale.getComponent("Message.No.AllowedHere", player));
            return;
        }
        if (!MyPetApi.getMyPetManager().hasActiveMyPet(player)) {
            player.sendMessage(Locale.getComponent("Message.No.HasPet", player));
            return;
        }

        final MyPet myPet = MyPetApi.getMyPetManager().getMyPet(player);
        final MyPetPlayer myPetOwner = myPet.getOwner();

        if (Configuration.Skilltree.AUTOMATIC_SKILLTREE_ASSIGNMENT && !myPet.getOwner().isMyPetAdmin()) {
            myPet.autoAssignSkilltree();
            player.sendMessage(Locale.getComponent("Message.Command.ChooseSkilltree.AutomaticSkilltreeAssignment", myPet.getOwner()));
            return;
        }

        List<Skilltree> availableSkilltrees = new ArrayList<>();
        for (Skilltree skilltree : MyPetApi.getSkilltreeManager().getOrderedSkilltrees()) {
            if (skilltree.getMobTypes().contains(myPet.getPetType()) && skilltree.checkRequirements(myPet)) {
                availableSkilltrees.add(skilltree);
            }
        }

        if (availableSkilltrees.isEmpty()) {
            player.sendMessage(Locale.getFormattedComponent("Message.Command.ChooseSkilltree.NoneAvailable", player, myPet.getDisplayName()));
            return;
        }

        final Map<Integer, Skilltree> skilltreeSlotMap = new HashMap<>();
        IconMenu menu = new IconMenu(Locale.getFormattedComponent("Message.Skilltree.Available", myPetOwner, myPet.getDisplayName()), event -> {
            if (myPet != myPetOwner.getMyPet()) {
                event.setWillClose(true);
                event.setWillDestroy(true);
                return;
            }
            if (myPet.getSkilltree() != null && Configuration.Skilltree.CHOOSE_SKILLTREE_ONLY_ONCE && !myPet.getOwner().isMyPetAdmin()) {
                player.sendMessage(Locale.getFormattedComponent("Message.Command.ChooseSkilltree.OnlyOnce", myPet.getOwner(), myPet.getDisplayName()));
            } else if (skilltreeSlotMap.containsKey(event.getPosition())) {
                Skilltree selectedSkilltree = skilltreeSlotMap.get(event.getPosition());
                if (selectedSkilltree != null) {
                    applySkilltree(myPet, myPetOwner, selectedSkilltree);
                }
            }
            event.setWillClose(true);
            event.setWillDestroy(true);
        }, MyPetApi.getPlugin()).setPaginationIdentifier("ChooseSkilltree");

        for (int i = 0; i < availableSkilltrees.size(); i++) {
            Skilltree addedSkilltree = availableSkilltrees.get(i);

            SkilltreeIcon icon = addedSkilltree.getIcon();
            Material material = Material.matchMaterial(icon.getMaterial());
            if (material == null) {
                material = Material.OAK_SAPLING;
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
                        .append(Locale.getFormattedComponent("Message.Skilltree.RequiresLevel.Item", myPetOwner.getLanguage(), requiredLevel).color(levelColor))
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

    /**
     * Assigns a skilltree to the player's pet by name, without opening the GUI.
     *
     * <p>Validates the same configuration constraints as {@link #executeGui(Player)}
     * (automatic assignment, choose-once). Looks up the skilltree by name and verifies
     * it is compatible with the pet's type and meets requirements before applying.</p>
     *
     * @param player        the player whose pet should receive the specified skilltree
     * @param skilltreeName the name of the skilltree to assign (case-sensitive match)
     */
    private void executeSkilltree(Player player, String skilltreeName) {
        if (WorldGroup.getGroupByWorld(player.getWorld()).isDisabled()) {
            player.sendMessage(Locale.getComponent("Message.No.AllowedHere", player));
            return;
        }
        if (!MyPetApi.getMyPetManager().hasActiveMyPet(player)) {
            player.sendMessage(Locale.getComponent("Message.No.HasPet", player));
            return;
        }

        final MyPet myPet = MyPetApi.getMyPetManager().getMyPet(player);
        final MyPetPlayer myPetOwner = myPet.getOwner();

        if (Configuration.Skilltree.AUTOMATIC_SKILLTREE_ASSIGNMENT && !myPet.getOwner().isMyPetAdmin()) {
            myPet.autoAssignSkilltree();
            player.sendMessage(Locale.getComponent("Message.Command.ChooseSkilltree.AutomaticSkilltreeAssignment", myPet.getOwner()));
            return;
        }
        if (myPet.getSkilltree() != null && Configuration.Skilltree.CHOOSE_SKILLTREE_ONLY_ONCE && !myPet.getOwner().isMyPetAdmin()) {
            player.sendMessage(Locale.getFormattedComponent("Message.Command.ChooseSkilltree.OnlyOnce", myPet.getOwner(), myPet.getDisplayName()));
            return;
        }
        if (MyPetApi.getSkilltreeManager().hasSkilltree(skilltreeName)) {
            Skilltree skilltree = MyPetApi.getSkilltreeManager().getSkilltree(skilltreeName);
            if (skilltree.getMobTypes().contains(myPet.getPetType()) && skilltree.checkRequirements(myPet)) {
                applySkilltree(myPet, myPetOwner, skilltree);
            } else {
                player.sendMessage(Locale.getFormattedComponent("Message.Command.Skilltree.CantFindSkilltree", player, skilltreeName));
            }
        } else {
            player.sendMessage(Locale.getFormattedComponent("Message.Command.Skilltree.CantFindSkilltree", player, skilltreeName));
        }
    }

    /**
     * Applies the given skilltree to the pet, handling level requirements and switch fees.
     *
     * <p>Validates that the pet meets the skilltree's minimum level requirement and does
     * not exceed its maximum level. On successful assignment, deducts an experience penalty
     * (fixed + percentage) unless the owner is an admin with fee exemption. The penalty
     * respects the {@link Configuration.LevelSystem.Experience#ALLOW_LEVEL_DOWNGRADE}
     * setting to determine whether the pet can lose levels from the fee.</p>
     *
     * @param myPet      the pet to assign the skilltree to
     * @param myPetOwner the pet's owner, used for localized messaging
     * @param skilltree  the skilltree to apply
     */
    private void applySkilltree(MyPet myPet, MyPetPlayer myPetOwner, Skilltree skilltree) {
        int requiredLevel = skilltree.getRequiredLevel();
        int maxLevel = skilltree.getMaxLevel();
        if (requiredLevel > 1 && myPet.getExperience().getLevel() < requiredLevel) {
            myPet.getOwner().sendMessage(Locale.getFormattedComponent("Message.Skilltree.RequiresLevel.Message", myPetOwner, myPet.getDisplayName(), requiredLevel));
        } else if (myPet.getExperience().getLevel() > maxLevel) {
            myPet.getOwner().sendMessage(Locale.getFormattedComponent("Message.Skilltree.MaxLevel.Message", myPetOwner, myPet.getDisplayName(), maxLevel));
        } else if (myPet.setSkilltree(skilltree, PetSelectSkilltreeEvent.Source.PlayerCommand)) {
            myPet.getOwner().sendMessage(Locale.getFormattedComponent("Message.Skilltree.SwitchedTo", myPetOwner, Util.SANITIZED_MINIMESSAGE.deserialize(skilltree.getDisplayName())));
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
            myPet.getOwner().sendMessage(Locale.getComponent("Message.Skilltree.NotSwitched", myPetOwner));
        }
    }
}

/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
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
import de.Keyle.MyPet.api.MyPetGlobal;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.event.PetSelectSkilltreeEvent;
import de.Keyle.MyPet.commands.help.CommandCategory;
import de.Keyle.MyPet.commands.help.HelpEntry;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.api.gui.MenuId;
import de.Keyle.MyPet.api.gui.MenuIds;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.skill.skilltree.Skilltree;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.gui.context.ChooseSkilltreeContext;
import io.papermc.paper.command.brigadier.Commands;
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
 *   <li>{@link MyPetGlobal.Skilltree#AUTOMATIC_SKILLTREE_ASSIGNMENT} — when enabled,
 *       skilltrees are auto-assigned and manual selection is blocked (unless admin)</li>
 *   <li>{@link MyPetGlobal.Skilltree#CHOOSE_SKILLTREE_ONLY_ONCE} — when enabled,
 *       players cannot change their pet's skilltree after the initial selection (unless admin)</li>
 * </ul>
 *
 * <p>Switching skilltrees may incur an experience penalty based on
 * {@link MyPetGlobal.Skilltree#SWITCH_FEE_FIXED} and
 * {@link MyPetGlobal.Skilltree#SWITCH_FEE_PERCENT}.</p>
 *
 * <p><b>Usage:</b> {@code /petchooseskilltree [skilltree]}</p>
 * <p><b>Aliases:</b> {@code /pcst}, {@code /petcst}</p>
 * <p><b>Help category:</b> {@link CommandCategory#SKILLS SKILLS} (priority 160)</p>
 *
 * @see Skilltree
 * @see PetSelectSkilltreeEvent
 */
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
                                        if (MyPetApi.getPetManager().hasActivePet(player)) {
                                            Pet pet = MyPetApi.getPetManager().getPet(player);
                                            for (Skilltree skilltree : MyPetApi.getSkilltreeManager().getOrderedSkilltrees()) {
                                                if (skilltree.getMobTypes().contains(pet.getPetType()) && skilltree.checkRequirements(pet)) {
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
                player -> MyPetApi.getPetManager().hasActivePet(player)
        ));
    }

    /**
     * Opens the skilltree selection GUI for the given player.
     *
     * <p>Opens the choose-skilltree menu displaying all skilltrees compatible with the pet's
     * type and meeting the pet's requirements (e.g., level). Clicking an entry applies
     * the selected skilltree.</p>
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
        if (!MyPetApi.getPetManager().hasActivePet(player)) {
            player.sendMessage(Locale.getComponent("Message.No.HasPet", player));
            return;
        }

        final Pet pet = MyPetApi.getPetManager().getPet(player);
        final MyPetPlayer myPetOwner = pet.getOwner();

        if (MyPetGlobal.Skilltree.AUTOMATIC_SKILLTREE_ASSIGNMENT.get() && !pet.getOwner().isMyPetAdmin()) {
            pet.autoAssignSkilltree();
            player.sendMessage(Locale.getComponent("Message.Command.ChooseSkilltree.AutomaticSkilltreeAssignment", pet.getOwner()));
            return;
        }

        List<Skilltree> availableSkilltrees = new ArrayList<>();
        for (Skilltree skilltree : MyPetApi.getSkilltreeManager().getOrderedSkilltrees()) {
            if (skilltree.getMobTypes().contains(pet.getPetType()) && skilltree.checkRequirements(pet)) {
                availableSkilltrees.add(skilltree);
            }
        }

        if (availableSkilltrees.isEmpty()) {
            player.sendMessage(Locale.getFormattedComponent("Message.Command.ChooseSkilltree.NoneAvailable", player, pet.getDisplayName()));
            return;
        }

        MyPetApi.getGuiService().openMenu(
                player,
                (MenuId<ChooseSkilltreeContext>) (MenuId<?>) MenuIds.CHOOSE_SKILLTREE,
                new ChooseSkilltreeContext(player, pet, availableSkilltrees, tree -> {
                    if (pet.getSkilltree() != null && MyPetGlobal.Skilltree.CHOOSE_SKILLTREE_ONLY_ONCE.get() && !pet.getOwner().isMyPetAdmin()) {
                        player.sendMessage(Locale.getFormattedComponent("Message.Command.ChooseSkilltree.OnlyOnce", pet.getOwner(), pet.getDisplayName()));
                        return;
                    }
                    applySkilltree(pet, myPetOwner, tree);
                }));
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
        if (!MyPetApi.getPetManager().hasActivePet(player)) {
            player.sendMessage(Locale.getComponent("Message.No.HasPet", player));
            return;
        }

        final Pet pet = MyPetApi.getPetManager().getPet(player);
        final MyPetPlayer myPetOwner = pet.getOwner();

        if (MyPetGlobal.Skilltree.AUTOMATIC_SKILLTREE_ASSIGNMENT.get() && !pet.getOwner().isMyPetAdmin()) {
            pet.autoAssignSkilltree();
            player.sendMessage(Locale.getComponent("Message.Command.ChooseSkilltree.AutomaticSkilltreeAssignment", pet.getOwner()));
            return;
        }
        if (pet.getSkilltree() != null && MyPetGlobal.Skilltree.CHOOSE_SKILLTREE_ONLY_ONCE.get() && !pet.getOwner().isMyPetAdmin()) {
            player.sendMessage(Locale.getFormattedComponent("Message.Command.ChooseSkilltree.OnlyOnce", pet.getOwner(), pet.getDisplayName()));
            return;
        }
        if (MyPetApi.getSkilltreeManager().hasSkilltree(skilltreeName)) {
            Skilltree skilltree = MyPetApi.getSkilltreeManager().getSkilltree(skilltreeName);
            if (skilltree.getMobTypes().contains(pet.getPetType()) && skilltree.checkRequirements(pet)) {
                applySkilltree(pet, myPetOwner, skilltree);
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
     * respects the {@link MyPetGlobal.LevelSystem.Experience#ALLOW_LEVEL_DOWNGRADE}
     * setting to determine whether the pet can lose levels from the fee.</p>
     *
     * @param pet      the pet to assign the skilltree to
     * @param myPetOwner the pet's owner, used for localized messaging
     * @param skilltree  the skilltree to apply
     */
    private void applySkilltree(Pet pet, MyPetPlayer myPetOwner, Skilltree skilltree) {
        int requiredLevel = skilltree.getRequiredLevel();
        int maxLevel = skilltree.getMaxLevel();
        if (requiredLevel > 1 && pet.getExperience().getLevel() < requiredLevel) {
            pet.getOwner().sendMessage(Locale.getFormattedComponent("Message.Skilltree.RequiresLevel.Message", myPetOwner, pet.getDisplayName(), requiredLevel));
        } else if (pet.getExperience().getLevel() > maxLevel) {
            pet.getOwner().sendMessage(Locale.getFormattedComponent("Message.Skilltree.MaxLevel.Message", myPetOwner, pet.getDisplayName(), maxLevel));
        } else if (pet.setSkilltree(skilltree, PetSelectSkilltreeEvent.Source.PLAYER_COMMAND)) {
            pet.getOwner().sendMessage(Locale.getFormattedComponent("Message.Skilltree.SwitchedTo", myPetOwner, Util.SANITIZED_MINIMESSAGE.deserialize(skilltree.getDisplayName())));
            if (!pet.getOwner().isMyPetAdmin() || MyPetGlobal.Skilltree.SWITCH_FEE_ADMIN.get()) {
                double switchPenalty = MyPetGlobal.Skilltree.SWITCH_FEE_FIXED.get();
                switchPenalty += pet.getExperience().getExp() * MyPetGlobal.Skilltree.SWITCH_FEE_PERCENT.get() / 100.;

                if (requiredLevel > 1) {
                    double minExp = pet.getExperience().getExpByLevel(requiredLevel);
                    switchPenalty = pet.getExp() - switchPenalty < minExp ? pet.getExp() - minExp : switchPenalty;
                }
                if (MyPetGlobal.LevelSystem.Experience.ALLOW_LEVEL_DOWNGRADE.get()) {
                    pet.getExperience().removeExp(switchPenalty);
                } else {
                    pet.getExperience().removeCurrentExp(switchPenalty);
                }
            }
        } else {
            pet.getOwner().sendMessage(Locale.getComponent("Message.Skilltree.NotSwitched", myPetOwner));
        }
    }
}

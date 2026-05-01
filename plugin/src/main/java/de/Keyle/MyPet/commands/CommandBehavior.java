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
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.commands.help.CommandCategory;
import de.Keyle.MyPet.commands.help.HelpEntry;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPet.PetState;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.skills.Behavior.BehaviorMode;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.skill.skills.BehaviorImpl;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handles the {@code /petbehavior} command, which sets or cycles through the behavior mode
 * of the player's active pet. Behavior modes control how the pet interacts with other entities:
 *
 * <ul>
 *   <li><b>normal</b> — default behavior, attacks what the owner attacks</li>
 *   <li><b>friendly</b> — pet will not attack anything</li>
 *   <li><b>aggressive</b> — pet attacks all nearby entities</li>
 *   <li><b>farm</b> — pet attacks only hostile mobs</li>
 *   <li><b>raid</b> — pet only attacks when the owner is attacked</li>
 *   <li><b>duel</b> — pet attacks other players' pets</li>
 * </ul>
 *
 * <p>When executed without arguments, cycles to the next available behavior mode.
 * When a mode name is provided, sets that specific mode directly.</p>
 *
 * <p><b>Usage:</b> {@code /petbehavior [mode]}</p>
 * <p><b>Aliases:</b> {@code /petbehaviour}, {@code /petb}, {@code /pb}</p>
 * <p><b>Permissions:</b> {@code MyPet.extended.behavior.<mode>} — required per mode
 * (e.g., {@code MyPet.extended.behavior.friendly}). The {@code normal} mode has no
 * permission requirement.</p>
 * <p><b>Help category:</b> {@link CommandCategory#SKILLS SKILLS} (priority 190)</p>
 *
 * @see BehaviorImpl
 * @see BehaviorMode
 */
@SuppressWarnings("UnstableApiUsage")
public class CommandBehavior {

    /** The list of recognized behavior mode names used for tab-completion suggestions. */
    private static final List<String> BEHAVIOR_MODES = List.of("friendly", "aggressive", "normal", "farm", "raid", "duel");

    /**
     * Registers the {@code /petbehavior} Brigadier command and its help entry.
     *
     * <p>The command tree consists of a base literal node (player-only) that cycles to the
     * next behavior mode, plus an optional {@code mode} string argument with tab-completion
     * for all supported behavior mode names.</p>
     *
     * @param commands     the Paper {@link Commands} registrar used to register the Brigadier command
     * @param helpRegistry the {@link HelpRegistry} to register the command's help entry with
     */
    public void register(Commands commands, HelpRegistry helpRegistry) {
        commands.register(
                Commands.literal("petbehavior")
                        .requires(ctx -> ctx.getSender() instanceof Player)
                        .executes(ctx -> {
                            executeNoArg((Player) ctx.getSource().getSender());
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    for (String mode : BEHAVIOR_MODES) {
                                        builder.suggest(mode);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    Player player = (Player) ctx.getSource().getSender();
                                    String mode = StringArgumentType.getString(ctx, "mode");
                                    execute(player, mode);
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .build(),
                "Set pet behavior mode",
                List.of("petbehaviour", "petb", "pb")
        );

        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.Behavior",
                "/petbehavior",
                CommandCategory.SKILLS,
                190,
                player -> MyPetApi.getMyPetManager().hasActiveMyPet(player)
                        && MyPetApi.getMyPetManager().getMyPet(player).getSkills().isActive(BehaviorImpl.class)
        ));
    }

    /**
     * Executes the behavior command with no arguments, cycling to the next behavior mode.
     *
     * <p>Validates that the world group is enabled, the player has an active and spawned pet,
     * and that the pet has the Behavior skill. If valid, activates the skill which cycles
     * through the available modes.</p>
     *
     * @param petOwner the player whose pet's behavior mode should be cycled
     */
    private void executeNoArg(Player petOwner) {
        if (WorldGroup.getGroupByWorld(petOwner.getWorld()).isDisabled()) {
            petOwner.sendMessage(Translation.getComponent("Message.No.AllowedHere", petOwner));
            return;
        }
        if (MyPetApi.getMyPetManager().hasActiveMyPet(petOwner)) {
            MyPet myPet = MyPetApi.getMyPetManager().getMyPet(petOwner);
            if (myPet.getStatus() == PetState.Despawned) {
                petOwner.sendMessage(Translation.getFormattedComponent("Message.Call.First", petOwner, myPet.getDisplayName()));
                return;
            }
            if (myPet.getStatus() == PetState.Dead) {
                petOwner.sendMessage(Translation.getFormattedComponent("Message.No.CanUse", petOwner, myPet.getDisplayName()));
                return;
            }
            if (myPet.getSkills().has(BehaviorImpl.class)) {
                BehaviorImpl behaviorSkill = myPet.getSkills().get(BehaviorImpl.class);
                behaviorSkill.activate();
            }
        } else {
            petOwner.sendMessage(Translation.getComponent("Message.No.HasPet", petOwner));
        }
    }

    /**
     * Executes the behavior command with a specific mode argument.
     *
     * <p>Validates the same preconditions as {@link #executeNoArg(Player)}, then checks
     * whether the player has the extended permission for the requested mode
     * ({@code MyPet.extended.behavior.<mode>}) and whether the mode is usable on the
     * pet's Behavior skill. If the mode name is unrecognized, falls back to cycling
     * behavior. Sends a confirmation message with the new mode name.</p>
     *
     * @param petOwner the player whose pet's behavior mode should be changed
     * @param mode     the desired behavior mode name (case-insensitive); one of
     *                 {@code friendly}, {@code aggressive}, {@code normal}, {@code farm},
     *                 {@code raid}, or {@code duel}
     */
    private void execute(Player petOwner, String mode) {
        if (WorldGroup.getGroupByWorld(petOwner.getWorld()).isDisabled()) {
            petOwner.sendMessage(Translation.getComponent("Message.No.AllowedHere", petOwner));
            return;
        }
        if (MyPetApi.getMyPetManager().hasActiveMyPet(petOwner)) {
            MyPet myPet = MyPetApi.getMyPetManager().getMyPet(petOwner);
            if (myPet.getStatus() == PetState.Despawned) {
                petOwner.sendMessage(Translation.getFormattedComponent("Message.Call.First", petOwner, myPet.getDisplayName()));
                return;
            }
            if (myPet.getStatus() == PetState.Dead) {
                petOwner.sendMessage(Translation.getFormattedComponent("Message.No.CanUse", petOwner, myPet.getDisplayName()));
                return;
            }
            if (myPet.getSkills().has(BehaviorImpl.class)) {
                BehaviorImpl behaviorSkill = myPet.getSkills().get(BehaviorImpl.class);

                if (mode.equalsIgnoreCase("friendly") || mode.equalsIgnoreCase("friend")) {
                    if (!Permissions.hasExtended(petOwner, "MyPet.extended.behavior.friendly") || !behaviorSkill.isModeUsable(BehaviorMode.Friendly)) {
                        myPet.getOwner().sendMessage(Translation.getComponent("Message.No.Allowed", petOwner));
                        return;
                    }
                    behaviorSkill.setBehavior(BehaviorMode.Friendly);
                } else if (mode.equalsIgnoreCase("aggressive") || mode.equalsIgnoreCase("aggro")) {
                    if (!Permissions.hasExtended(petOwner, "MyPet.extended.behavior.aggressive") || !behaviorSkill.isModeUsable(BehaviorMode.Aggressive)) {
                        myPet.getOwner().sendMessage(Translation.getComponent("Message.No.Allowed", petOwner));
                        return;
                    }
                    behaviorSkill.setBehavior(BehaviorMode.Aggressive);
                } else if (mode.equalsIgnoreCase("farm")) {
                    if (!Permissions.hasExtended(petOwner, "MyPet.extended.behavior.farm") || !behaviorSkill.isModeUsable(BehaviorMode.Farm)) {
                        myPet.getOwner().sendMessage(Translation.getComponent("Message.No.Allowed", petOwner));
                        return;
                    }
                    behaviorSkill.setBehavior(BehaviorMode.Farm);
                } else if (mode.equalsIgnoreCase("raid")) {
                    if (!Permissions.hasExtended(petOwner, "MyPet.extended.behavior.raid") || !behaviorSkill.isModeUsable(BehaviorMode.Raid)) {
                        myPet.getOwner().sendMessage(Translation.getComponent("Message.No.Allowed", petOwner));
                        return;
                    }
                    behaviorSkill.setBehavior(BehaviorMode.Raid);
                } else if (mode.equalsIgnoreCase("duel")) {
                    if (!Permissions.hasExtended(petOwner, "MyPet.extended.behavior.duel") || !behaviorSkill.isModeUsable(BehaviorMode.Duel)) {
                        myPet.getOwner().sendMessage(Translation.getComponent("Message.No.Allowed", petOwner));
                        return;
                    }
                    behaviorSkill.setBehavior(BehaviorMode.Duel);
                } else if (mode.equalsIgnoreCase("normal")) {
                    behaviorSkill.setBehavior(BehaviorMode.Normal);
                } else {
                    behaviorSkill.activate();
                    return;
                }
                myPet.getOwner().sendMessage(Translation.getFormattedComponent("Message.Skill.Behavior.NewMode", myPet.getOwner(), myPet.getDisplayName(), Translation.getComponent("Name." + behaviorSkill.getBehavior().name(), myPet.getOwner().getPlayer())));
            }
        } else {
            petOwner.sendMessage(Translation.getComponent("Message.No.HasPet", petOwner));
        }
    }
}

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
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.commands.CommandCategory;
import de.Keyle.MyPet.api.commands.HelpEntry;
import de.Keyle.MyPet.api.commands.HelpRegistry;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPet.PetState;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.skill.skills.BeaconImpl;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handles the {@code /petbeacon} command, which opens the Beacon skill's GUI for the
 * player's active pet. The Beacon skill provides area-of-effect buff selection (similar
 * to vanilla beacon effects) that the pet applies to nearby players.
 *
 * <p><b>Usage:</b> {@code /petbeacon}</p>
 * <p><b>Aliases:</b> {@code /pbeacon}</p>
 * <p><b>Permissions:</b> {@code MyPet.extended.beacon} — required to open the beacon GUI</p>
 * <p><b>Help category:</b> {@link CommandCategory#SKILLS SKILLS} (priority 200)</p>
 *
 * @see BeaconImpl
 */
@SuppressWarnings("UnstableApiUsage")
public class CommandBeacon {

    /**
     * Registers the {@code /petbeacon} Brigadier command and its help entry.
     *
     * <p>The command is a player-only literal with no arguments. The help entry is only
     * shown when the player has an active pet with the Beacon skill active.</p>
     *
     * @param commands     the Paper {@link Commands} registrar used to register the Brigadier command
     * @param helpRegistry the {@link HelpRegistry} to register the command's help entry with
     */
    public void register(Commands commands, HelpRegistry helpRegistry) {
        commands.register(
                Commands.literal("petbeacon")
                        .requires(ctx -> ctx.getSender() instanceof Player)
                        .executes(ctx -> {
                            execute((Player) ctx.getSource().getSender());
                            return Command.SINGLE_SUCCESS;
                        })
                        .build(),
                "Opens the beacon skill GUI",
                List.of("pbeacon")
        );

        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.Beacon",
                "/petbeacon",
                CommandCategory.SKILLS,
                200,
                player -> MyPetApi.getMyPetManager().hasActiveMyPet(player)
                        && MyPetApi.getMyPetManager().getMyPet(player).getSkills().isActive(BeaconImpl.class)
        ));
    }

    /**
     * Opens the Beacon skill GUI for the given player's pet.
     *
     * <p>Validates that the world group is enabled, the player has an active and spawned pet,
     * the player holds the {@code MyPet.extended.beacon} permission, and that the pet's Beacon
     * skill is active. If all checks pass, activates the Beacon skill which opens its GUI.</p>
     *
     * @param player the player whose pet's Beacon GUI should be opened
     */
    private void execute(Player player) {
        if (WorldGroup.getGroupByWorld(player.getWorld()).isDisabled()) {
            player.sendMessage(Translation.getComponent("Message.No.AllowedHere", player));
            return;
        }
        if (MyPetApi.getMyPetManager().hasActiveMyPet(player)) {
            MyPet myPet = MyPetApi.getMyPetManager().getMyPet(player);
            if (!Permissions.hasExtended(player, "MyPet.extended.beacon")) {
                myPet.getOwner().sendMessage(Translation.getComponent("Message.No.CanUse", player));
                return;
            }
            if (myPet.getStatus() == PetState.Despawned) {
                player.sendMessage(Translation.getFormattedComponent("Message.Call.First", player, myPet.getDisplayName()));
                return;
            }
            if (myPet.getStatus() == PetState.Dead) {
                player.sendMessage(Translation.getFormattedComponent("Message.Action.Dead", player, myPet.getDisplayName()));
                return;
            }
            if (myPet.getSkills().isActive(BeaconImpl.class)) {
                myPet.getSkills().get(BeaconImpl.class).activate();
            } else {
                player.sendMessage(Translation.getFormattedComponent("Message.No.Skill", player, myPet.getDisplayName(), Translation.getComponent("Name.Skill.Beacon", player)));
            }
        } else {
            player.sendMessage(Translation.getComponent("Message.No.HasPet", player));
        }
    }
}

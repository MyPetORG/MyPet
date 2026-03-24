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
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.util.locale.Translation;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handles the {@code /petstop} command, which forces the player's active pet to stop
 * attacking its current target. The pet forgets its target and returns to an idle state.
 *
 * <p><b>Usage:</b> {@code /petstop}</p>
 * <p><b>Aliases:</b> {@code /pets}, {@code /ps}</p>
 * <p><b>Help category:</b> {@link CommandCategory#PET PET} (priority 70)</p>
 */
@SuppressWarnings("UnstableApiUsage")
public class CommandStop {

    /**
     * Registers the {@code /petstop} Brigadier command and its help entry.
     *
     * <p>The command is a simple player-only literal with no arguments. The help entry
     * is only shown when the player has an active pet.</p>
     *
     * @param commands     the Paper {@link Commands} registrar used to register the Brigadier command
     * @param helpRegistry the {@link HelpRegistry} to register the command's help entry with
     */
    public void register(Commands commands, HelpRegistry helpRegistry) {
        commands.register(
                Commands.literal("petstop")
                        .requires(ctx -> ctx.getSender() instanceof Player)
                        .executes(ctx -> {
                            execute((Player) ctx.getSource().getSender());
                            return Command.SINGLE_SUCCESS;
                        })
                        .build(),
                "Stops your pet from attacking",
                List.of("pets", "ps")
        );

        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.Stop",
                "/petstop",
                CommandCategory.PET,
                70,
                player -> MyPetApi.getMyPetManager().hasActiveMyPet(player)
        ));
    }

    /**
     * Executes the stop-attack logic for the given player's pet.
     *
     * <p>Checks that the player's world group is not disabled, that the player has
     * an active pet, and that the pet is currently spawned (not despawned or dead).
     * If valid, tells the pet entity to forget its current attack target.</p>
     *
     * @param petOwner the player whose pet should stop attacking
     */
    private void execute(Player petOwner) {
        if (WorldGroup.getGroupByWorld(petOwner.getWorld()).isDisabled()) {
            petOwner.sendMessage(Translation.getComponent("Message.No.AllowedHere", petOwner));
            return;
        }
        if (MyPetApi.getMyPetManager().hasActiveMyPet(petOwner)) {
            MyPet myPet = MyPetApi.getMyPetManager().getMyPet(petOwner);

            if (myPet.getStatus() == PetState.Despawned) {
                petOwner.sendMessage(Translation.getFormattedComponent("Message.Call.First", petOwner, myPet.getDisplayName()));
                return;
            } else if (myPet.getStatus() == PetState.Dead) {
                petOwner.sendMessage(Translation.getFormattedComponent("Message.Action.Dead", petOwner, myPet.getDisplayName()));
                return;
            }
            petOwner.sendMessage(Translation.getFormattedComponent("Message.Command.Stop.Attack", petOwner, myPet.getDisplayName()));
            myPet.getEntity().ifPresent(MyPetBukkitEntity::forgetTarget);
        } else {
            petOwner.sendMessage(Translation.getComponent("Message.No.HasPet", petOwner));
        }
    }
}

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
import de.Keyle.MyPet.commands.help.CommandCategory;
import de.Keyle.MyPet.commands.help.HelpEntry;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPet.PetState;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.skill.skills.PickupImpl;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handles the {@code /petpickup} command, which toggles the Pickup skill on the player's
 * active pet. When enabled, the pet automatically collects nearby dropped items for its owner.
 *
 * <p><b>Usage:</b> {@code /petpickup}</p>
 * <p><b>Aliases:</b> {@code /petp}, {@code /pp}</p>
 * <p><b>Permissions:</b> {@code MyPet.extended.pickup} — required to use the pickup toggle</p>
 * <p><b>Help category:</b> {@link CommandCategory#SKILLS SKILLS} (priority 150)</p>
 *
 * @see PickupImpl
 */
@SuppressWarnings("UnstableApiUsage")
public class CommandPickup {

    /**
     * Registers the {@code /petpickup} Brigadier command and its help entry.
     *
     * <p>The command is a player-only literal with no arguments. The help entry is only
     * shown when the player has an active pet with the Pickup skill active.</p>
     *
     * @param commands     the Paper {@link Commands} registrar used to register the Brigadier command
     * @param helpRegistry the {@link HelpRegistry} to register the command's help entry with
     */
    public void register(Commands commands, HelpRegistry helpRegistry) {
        commands.register(
                Commands.literal("petpickup")
                        .requires(ctx -> ctx.getSender() instanceof Player)
                        .executes(ctx -> {
                            execute((Player) ctx.getSource().getSender());
                            return Command.SINGLE_SUCCESS;
                        })
                        .build(),
                "Toggles pet pickup",
                List.of("petp", "pp")
        );

        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.Pickup",
                "/petpickup",
                CommandCategory.SKILLS,
                150,
                player -> MyPetApi.getMyPetManager().hasActiveMyPet(player)
                        && MyPetApi.getMyPetManager().getMyPet(player).getSkills().isActive(PickupImpl.class)
        ));
    }

    /**
     * Executes the pickup toggle for the given player's pet.
     *
     * <p>Validates that the world group is enabled, the player has an active and spawned pet,
     * and that the player holds the {@code MyPet.extended.pickup} permission. If all checks
     * pass, activates (toggles) the Pickup skill on the pet.</p>
     *
     * @param owner the player whose pet's Pickup skill should be toggled
     */
    private void execute(Player owner) {
        if (WorldGroup.getGroupByWorld(owner.getWorld()).isDisabled()) {
            owner.sendMessage(Translation.getComponent("Message.No.AllowedHere", owner));
            return;
        }
        if (MyPetApi.getMyPetManager().hasActiveMyPet(owner)) {
            MyPet myPet = MyPetApi.getMyPetManager().getMyPet(owner);

            if (!Permissions.hasExtended(myPet.getOwner().getPlayer(), "MyPet.extended.pickup")) {
                owner.sendMessage(Translation.getComponent("Message.No.Allowed", owner));
                return;
            } else if (myPet.getStatus() == PetState.Despawned) {
                owner.sendMessage(Translation.getFormattedComponent("Message.Call.First", owner, myPet.getDisplayName()));
                return;
            } else if (myPet.getStatus() == PetState.Dead) {
                owner.sendMessage(Translation.getFormattedComponent("Message.Action.Dead", owner, myPet.getDisplayName()));
                return;
            }
            if (myPet.getSkills().has(PickupImpl.class)) {
                myPet.getSkills().get(PickupImpl.class).activate();
            }
        } else {
            owner.sendMessage(Translation.getComponent("Message.No.HasPet", owner));
        }
    }
}

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
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.config.ConfigKeyRegistry;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.Pet.PetState;
import de.Keyle.MyPet.api.entity.PetMultiPassenger;
import de.Keyle.MyPet.api.entity.PetNaturallyRideable;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.commands.help.CommandCategory;
import de.Keyle.MyPet.commands.help.HelpEntry;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.listeners.RideGate;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handles the {@code /petride} command and its subcommands.
 *
 * <p><b>Usage:</b>
 * <ul>
 *   <li>{@code /petride} — mounts the player on their active pet (requires the Ride skill).</li>
 *   <li>{@code /petride passengers allow} — opts the owner's pet into accepting non-owner passengers (default).</li>
 *   <li>{@code /petride passengers disallow} — locks the owner's pet to single-rider use.</li>
 * </ul>
 *
 * <p>The passengers toggle is stored as a PDC byte on the live Bukkit mob via
 * {@link RideGate#ALLOW_PASSENGERS_KEY} and persists across despawn/respawn via
 * {@code PetEntitySnapshot}'s full-NBT round-trip. The owner toggle ANDs with the
 * admin-side {@code AllowNonOwnerSecondaryMount} flag — admin denial wins.
 *
 * <p>{@code /petride} (no args) is treated as an explicit owner-driven ride trigger
 * that runs the full {@link RideGate#evaluate} gate chain except for the ride-item
 * check: holding the command itself is the trigger, so the player does not need
 * to hold the configured {@code Skilltree.Skill.Ride.RIDE_ITEM}.
 */
/*
 * Multi-Pet Phase 2 (MyPetORG/MyPet#1435): this command resolves the player to a
 * single Pet via the manager. That has no unambiguous answer once a player can
 * have several out -- it needs the optional pet-name argument the issue calls for,
 * so it is deliberately left alone until that argument exists.
 */
public class CommandPetRide {

    /**
     * Registers the {@code /petride} Brigadier command, its subcommands, and the help entry.
     */
    public void register(Commands commands, HelpRegistry helpRegistry) {
        commands.register(
                Commands.literal("petride")
                        .requires(ctx -> ctx.getSender() instanceof Player)
                        .executes(ctx -> {
                            executeMount((Player) ctx.getSource().getSender());
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.literal("passengers")
                                .then(Commands.literal("allow")
                                        .executes(ctx -> {
                                            executePassengersToggle((Player) ctx.getSource().getSender(), true);
                                            return Command.SINGLE_SUCCESS;
                                        }))
                                .then(Commands.literal("disallow")
                                        .executes(ctx -> {
                                            executePassengersToggle((Player) ctx.getSource().getSender(), false);
                                            return Command.SINGLE_SUCCESS;
                                        })))
                        .then(Commands.literal("driver")
                                .then(Commands.literal("allow")
                                        .executes(ctx -> {
                                            executeDriverToggle((Player) ctx.getSource().getSender(), true);
                                            return Command.SINGLE_SUCCESS;
                                        }))
                                .then(Commands.literal("disallow")
                                        .executes(ctx -> {
                                            executeDriverToggle((Player) ctx.getSource().getSender(), false);
                                            return Command.SINGLE_SUCCESS;
                                        })))
                        .build(),
                "Mount your active pet",
                List.of("ride", "pr")
        );

        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.Ride",
                "/petride",
                CommandCategory.PET,
                65,
                player -> MyPetApi.getPetManager().hasActivePet(player)
        ));
        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.Ride.Passengers",
                "/petride passengers <allow|disallow>",
                CommandCategory.PET,
                66,
                player -> MyPetApi.getPetManager().hasActivePet(player)
                        && MyPetApi.getPetManager().getPet(player) instanceof PetMultiPassenger
        ));
        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.Ride.Driver",
                "/petride driver <allow|disallow>",
                CommandCategory.PET,
                67,
                player -> MyPetApi.getPetManager().hasActivePet(player)
                        && MyPetApi.getPetManager().getPet(player) instanceof PetNaturallyRideable
        ));
    }

    /**
     * Resolves the player's active pet for any {@code /petride} subcommand, sending
     * the standard rejection messages if any gate fails. Returns {@code null} on
     * rejection (caller bails); returns the pet on success.
     */
    private Pet resolveActiveRideablePet(Player petOwner) {
        if (WorldGroup.getGroupByWorld(petOwner.getWorld()).isDisabled()) {
            petOwner.sendMessage(Locale.getComponent("Message.No.AllowedHere", petOwner));
            return null;
        }
        if (!MyPetApi.getPetManager().hasActivePet(petOwner)) {
            petOwner.sendMessage(Locale.getComponent("Message.No.HasPet", petOwner));
            return null;
        }
        Pet pet = MyPetApi.getPetManager().getPet(petOwner);
        if (pet.getStatus() == PetState.Despawned) {
            petOwner.sendMessage(Locale.getFormattedComponent("Message.Call.First", petOwner, pet.getDisplayName()));
            return null;
        }
        if (pet.getStatus() == PetState.Dead) {
            petOwner.sendMessage(Locale.getFormattedComponent("Message.Action.Dead", petOwner, pet.getDisplayName()));
            return null;
        }
        if (!RideGate.isMountable(pet)) {
            petOwner.sendMessage(Locale.getFormattedComponent("Message.No.Skill", petOwner,
                    pet.getDisplayName(),
                    Locale.getComponent("Name.Skill.Ride", petOwner)));
            return null;
        }
        return pet;
    }

    /**
     * Mounts the player on their active pet.
     *
     * <p>Runs the full {@link RideGate#evaluate} gate chain except the ride-item
     * check (the command itself is the explicit ride trigger). The mob's world
     * must match the player's; cross-world mounts are rejected with the standard
     * "pet not here" message.
     */
    private void executeMount(Player petOwner) {
        Pet pet = resolveActiveRideablePet(petOwner);
        if (pet == null) return;

        Mob mob = pet.getBukkitEntity();
        if (mob == null) return;
        if (!mob.getWorld().equals(petOwner.getWorld())) {
            petOwner.sendMessage(Locale.getFormattedComponent("Message.Call.First", petOwner, pet.getDisplayName()));
            return;
        }
        if (mob.getPassengers().contains(petOwner)) {
            return;  // already on board
        }
        if (!pet.canMove()) {
            petOwner.sendMessage(Locale.getFormattedComponent("Message.Action.Dead", petOwner, pet.getDisplayName()));
            return;
        }

        boolean isDriverSeat = mob.getPassengers().isEmpty();
        // Mark as command-driven so RideGate.evaluate skips the RequireRideItem check.
        RideGate.Rejection[] rejectionHolder = new RideGate.Rejection[1];
        RideGate.runAsCommandTrigger(() -> {
            rejectionHolder[0] = RideGate.evaluate(pet, mob, petOwner, true, isDriverSeat);
        });
        RideGate.Rejection rejection = rejectionHolder[0];

        if (rejection != null) {
            RideGate.sendRejectionMessage(pet, petOwner, rejection, true);
            return;
        }

        RideGate.approve(mob, petOwner);
    }

    /**
     * Sets the owner's per-pet "allow non-owner driver" PDC toggle.
     *
     * <p>Mirrors {@link #executePassengersToggle} but applies to the primary
     * (driver) seat. Available on every {@link PetNaturallyRideable} pet — the
     * "MultiPassenger" gate from the passengers subcommand does not apply
     * because every rideable pet has a driver seat. The change persists via
     * {@code PetEntitySnapshot}'s full-NBT round-trip on next save.
     */
    private void executeDriverToggle(Player petOwner, boolean allowed) {
        Pet pet = resolveActiveRideablePet(petOwner);
        if (pet == null) return;

        Mob mob = pet.getBukkitEntity();
        if (mob == null) return;
        if (!mob.getWorld().equals(petOwner.getWorld())) {
            petOwner.sendMessage(Locale.getFormattedComponent("Message.Call.First", petOwner, pet.getDisplayName()));
            return;
        }

        // Always write the PDC even when admin has disabled the corresponding
        // admin flag — that way if the admin re-enables it later, the owner's
        // prior opt-in choice is preserved without requiring them to re-run
        // the command.
        RideGate.setDriverAllowed(mob, allowed);

        // When the owner opts in but admin has disabled non-owner primary
        // mounting for this pet type, the opt-in has no current effect.
        // Surface that explicitly so the owner isn't left wondering why their
        // friends still can't mount.
        if (allowed && !ConfigKeyRegistry.readBool(pet.getPetType().name(), "AllowNonOwnerPrimaryMount", false)) {
            petOwner.sendMessage(Locale.getFormattedComponent(
                    "Message.Command.Ride.Driver.AdminDisabled", petOwner,
                    pet.getPetType().name()));
            return;
        }

        String key = allowed
                ? "Message.Command.Ride.Driver.Allowed"
                : "Message.Command.Ride.Driver.Disallowed";
        petOwner.sendMessage(Locale.getFormattedComponent(key, petOwner, pet.getDisplayName()));
    }

    /**
     * Sets the owner's per-pet "allow passengers" PDC toggle.
     *
     * <p>Requires the pet to be in the same world as the owner so the PDC write
     * targets the live mob. The change persists via {@code PetEntitySnapshot}'s
     * full-NBT round-trip on next save.
     */
    private void executePassengersToggle(Player petOwner, boolean allowed) {
        Pet pet = resolveActiveRideablePet(petOwner);
        if (pet == null) return;

        if (!(pet instanceof PetMultiPassenger)) {
            petOwner.sendMessage(Locale.getFormattedComponent("Message.Command.Ride.NotMultiPassenger", petOwner,
                    pet.getDisplayName()));
            return;
        }

        Mob mob = pet.getBukkitEntity();
        if (mob == null) return;
        if (!mob.getWorld().equals(petOwner.getWorld())) {
            petOwner.sendMessage(Locale.getFormattedComponent("Message.Call.First", petOwner, pet.getDisplayName()));
            return;
        }

        RideGate.setPassengersAllowed(mob, allowed);

        // When the owner opts in but admin has disabled non-owner secondary
        // mounting for this pet type, the opt-in has no current effect.
        // Same shape as the driver case — see executeDriverToggle.
        if (allowed && !ConfigKeyRegistry.readBool(pet.getPetType().name(), "AllowNonOwnerSecondaryMount", true)) {
            petOwner.sendMessage(Locale.getFormattedComponent(
                    "Message.Command.Ride.Passengers.AdminDisabled", petOwner,
                    pet.getPetType().name()));
            return;
        }

        String key = allowed
                ? "Message.Command.Ride.Passengers.Allowed"
                : "Message.Command.Ride.Passengers.Disallowed";
        petOwner.sendMessage(Locale.getFormattedComponent(key, petOwner, pet.getDisplayName()));
    }

}

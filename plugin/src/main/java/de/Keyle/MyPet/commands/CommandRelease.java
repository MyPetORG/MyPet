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
import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.PetEquipment;
import de.Keyle.MyPet.api.entity.PetInfo;
import de.Keyle.MyPet.api.event.PetRemoveEvent;
import de.Keyle.MyPet.commands.help.CommandCategory;
import de.Keyle.MyPet.commands.help.HelpEntry;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.api.entity.Pet.PetState;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.skills.Backpack;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.entity.spawn.VanillaMobSpawner;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handles the {@code /petrelease} command (no aliases).
 *
 * <p>Permanently releases the player's active pet. When invoked without arguments, a
 * confirmation prompt is shown with a clickable pet name. The player must then confirm
 * by running {@code /petrelease <pet-name>} with the exact pet name (MiniMessage tags
 * stripped). Upon confirmation, the pet is removed from the database, its Backpack
 * contents and equipment are dropped, and optionally a vanilla entity is spawned in
 * its place (controlled by {@link PetInfo#getRemoveAfterRelease}).</p>
 *
 * <p>This command is restricted to in-game players only (no console support).</p>
 *
 * <p><b>Usage:</b> {@code /petrelease [name]}</p>
 *
 * <p><b>Permissions:</b></p>
 * <ul>
 *   <li>{@code MyPet.command.release} -- required to release a pet</li>
 * </ul>
 */
public class CommandRelease {
    /**
     * Registers the {@code /petrelease} Brigadier command and its help entry.
     *
     * <p>The command accepts an optional greedy string argument for the pet name
     * confirmation.</p>
     *
     * @param commands     the Paper {@link Commands} registrar used to register the Brigadier command
     * @param helpRegistry the {@link HelpRegistry} to register the command's help entry with
     */
    public void register(Commands commands, HelpRegistry helpRegistry) {
        commands.register(
                Commands.literal("petrelease")
                        .requires(ctx -> ctx.getSender() instanceof Player)
                        .executes(ctx -> {
                            Player player = (Player) ctx.getSource().getSender();
                            executeNoArgs(player);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    Player player = (Player) ctx.getSource().getSender();
                                    String name = StringArgumentType.getString(ctx, "name");
                                    executeWithName(player, name);
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .build(),
                "Releases your pet",
                List.of()
        );

        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.Release",
                "/petrelease",
                CommandCategory.PET,
                100,
                player -> MyPetApi.getPetManager().hasActivePet(player)
                        && Permissions.has(player, "MyPet.command.release")
        ));
    }

    /**
     * Handles the command when invoked without a pet name argument. Validates that the
     * player has an active, spawned pet and then shows the release confirmation prompt.
     *
     * @param petOwner the player executing the command
     */
    private void executeNoArgs(Player petOwner) {
        if (WorldGroup.getGroupByWorld(petOwner.getWorld()).isDisabled()) {
            petOwner.sendMessage(Locale.getComponent("Message.No.AllowedHere", petOwner));
            return;
        }
        if (!MyPetApi.getPetManager().hasActivePet(petOwner)) {
            petOwner.sendMessage(Locale.getComponent("Message.No.HasPet", petOwner));
            return;
        }

        Pet pet = MyPetApi.getPetManager().getPet(petOwner);
        if (!Permissions.has(petOwner, "MyPet.command.release")) {
            return;
        }
        if (pet.getStatus() == PetState.Despawned) {
            petOwner.sendMessage(Locale.getFormattedComponent("Message.Call.First", petOwner, pet.getDisplayName()));
            return;
        } else if (pet.getStatus() == PetState.Dead) {
            petOwner.sendMessage(Locale.getFormattedComponent("Message.Spawn.Respawn.In", petOwner, pet.getDisplayName(), pet.getRespawnTime()));
            return;
        }

        showReleasePrompt(petOwner, pet);
    }

    /**
     * Handles the command when invoked with a pet name argument. If the name matches
     * the active pet's name (case-insensitive, MiniMessage tags stripped), the pet is
     * released: a {@link PetRemoveEvent} is fired, a vanilla entity may be spawned,
     * Backpack contents and equipment are dropped, and the pet is permanently deleted.
     * If the name does not match, the confirmation prompt is shown again.
     *
     * @param petOwner the player executing the command
     * @param name     the pet name provided as confirmation
     */
    private void executeWithName(Player petOwner, String name) {
        if (WorldGroup.getGroupByWorld(petOwner.getWorld()).isDisabled()) {
            petOwner.sendMessage(Locale.getComponent("Message.No.AllowedHere", petOwner));
            return;
        }
        if (!MyPetApi.getPetManager().hasActivePet(petOwner)) {
            petOwner.sendMessage(Locale.getComponent("Message.No.HasPet", petOwner));
            return;
        }

        Pet pet = MyPetApi.getPetManager().getPet(petOwner);
        if (!Permissions.has(petOwner, "MyPet.command.release")) {
            return;
        }
        if (pet.getStatus() == PetState.Despawned) {
            petOwner.sendMessage(Locale.getFormattedComponent("Message.Call.First", petOwner, pet.getDisplayName()));
            return;
        } else if (pet.getStatus() == PetState.Dead) {
            petOwner.sendMessage(Locale.getFormattedComponent("Message.Spawn.Respawn.In", petOwner, pet.getDisplayName(), pet.getRespawnTime()));
            return;
        }

        if (Util.SANITIZED_MINIMESSAGE.stripTags(pet.getPetName()).trim().equalsIgnoreCase(name.trim())) {
            PetRemoveEvent removeEvent = new PetRemoveEvent(pet, PetRemoveEvent.Source.RELEASE);
            Bukkit.getServer().getPluginManager().callEvent(removeEvent);

            // Drop backpack contents BEFORE releaseToWild — that call detaches
            // the Bukkit entity reference from the Pet, after which
            // pet.getLocation() falls back to the owner's location (see
            // Pet#getLocation) and items would drop at the owner's feet
            // instead of where the pet is standing.
            if (pet.getSkills().isActive(Backpack.class)) {
                pet.getSkills().get(Backpack.class).getInventory().dropContentAt(pet.getLocation().get());
            }

            boolean entityConverted = false;
            if (!MyPetApi.getPetInfo().getRemoveAfterRelease(pet.getPetType())) {
                // The pet IS a real vanilla mob — just strip MyPet infrastructure
                // and release it back to the wild. No need to spawn a new entity.
                try {
                    new VanillaMobSpawner().releaseToWild(pet);
                    entityConverted = true;
                } catch (Exception e) {
                    // Log the exception and notify the owner. releaseToWild is
                    // exception-safe w.r.t. the Pet domain object (it detaches
                    // state before any throwing operation), so the caller can
                    // still finish the rest of the cleanup path below.
                    MyPetApi.getLogger().log(java.util.logging.Level.SEVERE,
                            "Failed to release pet " + pet.getPetName() + " to wild", e);
                    petOwner.sendMessage(Component.text(
                                    "Failed to release your pet: " + e.getMessage())
                            .color(NamedTextColor.RED));
                    return;
                }
            }

            // Only drop equipment if the entity wasn't converted (equipment already transferred to the converted entity)
            if (pet instanceof PetEquipment && !entityConverted) {
                ((PetEquipment) pet).dropEquipment();
            }

            pet.removePet();
            pet.getOwner().setPetForWorldGroup(WorldGroup.getGroupByWorld(petOwner.getWorld().getName()), null);

            petOwner.sendMessage(Locale.getFormattedComponent("Message.Command.Release.Success", petOwner, pet.getDisplayName()));
            MyPetApi.getPetManager().deactivatePet(pet.getOwner(), false);
            MyPetPlugin.getInstance().getRepository().removePet(pet.getUUID());
        } else {
            showReleasePrompt(petOwner, pet);
        }
    }

    /**
     * Sends the release confirmation prompt to the player. The prompt includes a clickable
     * pet name component that auto-runs the confirmation command, and a hover tooltip
     * showing pet stats (hunger, HP/respawn time, experience, type, and skilltree).
     *
     * @param petOwner the player to show the prompt to
     * @param pet    the active pet being released
     */
    private void showReleasePrompt(Player petOwner, Pet pet) {
        // Build hover item with pet stats
        TextComponent.Builder hoverBuilder = Component.text();

        hoverBuilder.append(Locale.getComponent("Name.Hunger", petOwner))
                .append(Component.text(": "))
                .append(Component.text(Math.round(pet.getSaturation())).color(NamedTextColor.GOLD));

        if (pet.getRespawnTime() > 0) {
            hoverBuilder.append(Component.newline())
                    .append(Locale.getComponent("Name.Respawntime", petOwner))
                    .append(Component.text(": "))
                    .append(Component.text(pet.getRespawnTime() + "sec").color(NamedTextColor.GOLD));
        } else {
            hoverBuilder.append(Component.newline())
                    .append(Locale.getComponent("Name.HP", petOwner))
                    .append(Component.text(": "))
                    .append(Component.text(String.format("%1.2f", pet.getHealth())).color(NamedTextColor.GOLD));
        }

        hoverBuilder.append(Component.newline())
                .append(Locale.getComponent("Name.Exp", petOwner))
                .append(Component.text(": "))
                .append(Component.text(String.format("%1.2f", pet.getExp())).color(NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Locale.getComponent("Name.Type", petOwner))
                .append(Component.text(": "))
                .append(Locale.getComponent("Name." + pet.getPetType().name(), petOwner).color(NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Locale.getComponent("Name.Skilltree", petOwner))
                .append(Component.text(": "))
                .append(Util.SANITIZED_MINIMESSAGE.deserialize(pet.getSkilltree() != null ? pet.getSkilltree().getDisplayName() : "-")
                        .color(NamedTextColor.GOLD));

        HoverEvent<Component> hoverEvent = HoverEvent.showText(hoverBuilder.build());

        petOwner.sendMessage(
                Locale.getComponent("Message.Command.Release.Confirm", petOwner).append(Component.text(" "))
                        .append(
                                pet.getDisplayName()
                                        .clickEvent(ClickEvent.runCommand("/petrelease " + Util.SANITIZED_MINIMESSAGE.stripTags(pet.getPetName())))
                                        .hoverEvent(hoverEvent)
                        )
        );
    }
}

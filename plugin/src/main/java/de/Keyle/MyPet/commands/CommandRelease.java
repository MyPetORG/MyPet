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
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.commands.CommandCategory;
import de.Keyle.MyPet.api.commands.HelpEntry;
import de.Keyle.MyPet.api.commands.HelpRegistry;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPet.PetState;
import de.Keyle.MyPet.api.entity.MyPetEquipment;
import de.Keyle.MyPet.api.event.MyPetRemoveEvent;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.skill.skills.Backpack;
import de.Keyle.MyPet.api.util.locale.Translation;
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
 * its place (controlled by {@link de.Keyle.MyPet.api.entity.MyPetInfo#getRemoveAfterRelease}).</p>
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
@SuppressWarnings("UnstableApiUsage")
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
                player -> MyPetApi.getMyPetManager().hasActiveMyPet(player)
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
            petOwner.sendMessage(Translation.getComponent("Message.No.AllowedHere", petOwner));
            return;
        }
        if (!MyPetApi.getMyPetManager().hasActiveMyPet(petOwner)) {
            petOwner.sendMessage(Translation.getComponent("Message.No.HasPet", petOwner));
            return;
        }

        MyPet myPet = MyPetApi.getMyPetManager().getMyPet(petOwner);
        if (!Permissions.has(petOwner, "MyPet.command.release")) {
            return;
        }
        if (myPet.getStatus() == PetState.Despawned) {
            petOwner.sendMessage(Translation.getFormattedComponent("Message.Call.First", petOwner, myPet.getDisplayName()));
            return;
        } else if (myPet.getStatus() == PetState.Dead) {
            petOwner.sendMessage(Translation.getFormattedComponent("Message.Spawn.Respawn.In", petOwner, myPet.getDisplayName(), myPet.getRespawnTime()));
            return;
        }

        showReleasePrompt(petOwner, myPet);
    }

    /**
     * Handles the command when invoked with a pet name argument. If the name matches
     * the active pet's name (case-insensitive, MiniMessage tags stripped), the pet is
     * released: a {@link MyPetRemoveEvent} is fired, a vanilla entity may be spawned,
     * Backpack contents and equipment are dropped, and the pet is permanently deleted.
     * If the name does not match, the confirmation prompt is shown again.
     *
     * @param petOwner the player executing the command
     * @param name     the pet name provided as confirmation
     */
    private void executeWithName(Player petOwner, String name) {
        if (WorldGroup.getGroupByWorld(petOwner.getWorld()).isDisabled()) {
            petOwner.sendMessage(Translation.getComponent("Message.No.AllowedHere", petOwner));
            return;
        }
        if (!MyPetApi.getMyPetManager().hasActiveMyPet(petOwner)) {
            petOwner.sendMessage(Translation.getComponent("Message.No.HasPet", petOwner));
            return;
        }

        MyPet myPet = MyPetApi.getMyPetManager().getMyPet(petOwner);
        if (!Permissions.has(petOwner, "MyPet.command.release")) {
            return;
        }
        if (myPet.getStatus() == PetState.Despawned) {
            petOwner.sendMessage(Translation.getFormattedComponent("Message.Call.First", petOwner, myPet.getDisplayName()));
            return;
        } else if (myPet.getStatus() == PetState.Dead) {
            petOwner.sendMessage(Translation.getFormattedComponent("Message.Spawn.Respawn.In", petOwner, myPet.getDisplayName(), myPet.getRespawnTime()));
            return;
        }

        if (Util.SANITIZED_MINIMESSAGE.stripTags(myPet.getPetName()).trim().equalsIgnoreCase(name.trim())) {
            MyPetRemoveEvent removeEvent = new MyPetRemoveEvent(myPet, MyPetRemoveEvent.Source.Release);
            Bukkit.getServer().getPluginManager().callEvent(removeEvent);

            // Drop backpack contents BEFORE releaseToWild — that call detaches
            // the Bukkit entity reference from the MyPet, after which
            // myPet.getLocation() falls back to the owner's location (see
            // MyPet#getLocation) and items would drop at the owner's feet
            // instead of where the pet is standing.
            if (myPet.getSkills().isActive(Backpack.class)) {
                myPet.getSkills().get(Backpack.class).getInventory().dropContentAt(myPet.getLocation().get());
            }

            boolean entityConverted = false;
            if (!MyPetApi.getMyPetInfo().getRemoveAfterRelease(myPet.getPetType())) {
                // The pet IS a real vanilla mob — just strip MyPet infrastructure
                // and release it back to the wild. No need to spawn a new entity.
                try {
                    new VanillaMobSpawner().releaseToWild(myPet);
                    entityConverted = true;
                } catch (Exception e) {
                    // Log the exception and notify the owner. releaseToWild is
                    // exception-safe w.r.t. the MyPet domain object (it detaches
                    // state before any throwing operation), so the caller can
                    // still finish the rest of the cleanup path below.
                    MyPetApi.getLogger().log(java.util.logging.Level.SEVERE,
                            "Failed to release pet " + myPet.getPetName() + " to wild", e);
                    petOwner.sendMessage(Component.text(
                                    "Failed to release your pet: " + e.getMessage())
                            .color(NamedTextColor.RED));
                    return;
                }
            }

            // Only drop equipment if the entity wasn't converted (equipment already transferred to the converted entity)
            if (myPet instanceof MyPetEquipment && !entityConverted) {
                ((MyPetEquipment) myPet).dropEquipment();
            }

            myPet.removePet();
            myPet.getOwner().setMyPetForWorldGroup(WorldGroup.getGroupByWorld(petOwner.getWorld().getName()), null);

            petOwner.sendMessage(Translation.getFormattedComponent("Message.Command.Release.Success", petOwner, myPet.getDisplayName()));
            MyPetApi.getMyPetManager().deactivateMyPet(myPet.getOwner(), false);
            MyPetApi.getRepository().removePet(myPet.getUUID());
        } else {
            showReleasePrompt(petOwner, myPet);
        }
    }

    /**
     * Sends the release confirmation prompt to the player. The prompt includes a clickable
     * pet name component that auto-runs the confirmation command, and a hover tooltip
     * showing pet stats (hunger, HP/respawn time, experience, type, and skilltree).
     *
     * @param petOwner the player to show the prompt to
     * @param myPet    the active pet being released
     */
    private void showReleasePrompt(Player petOwner, MyPet myPet) {
        // Build hover item with pet stats
        TextComponent.Builder hoverBuilder = Component.text();

        hoverBuilder.append(Translation.getComponent("Name.Hunger", petOwner))
                .append(Component.text(": "))
                .append(Component.text(Math.round(myPet.getSaturation())).color(NamedTextColor.GOLD));

        if (myPet.getRespawnTime() > 0) {
            hoverBuilder.append(Component.newline())
                    .append(Translation.getComponent("Name.Respawntime", petOwner))
                    .append(Component.text(": "))
                    .append(Component.text(myPet.getRespawnTime() + "sec").color(NamedTextColor.GOLD));
        } else {
            hoverBuilder.append(Component.newline())
                    .append(Translation.getComponent("Name.HP", petOwner))
                    .append(Component.text(": "))
                    .append(Component.text(String.format("%1.2f", myPet.getHealth())).color(NamedTextColor.GOLD));
        }

        hoverBuilder.append(Component.newline())
                .append(Translation.getComponent("Name.Exp", petOwner))
                .append(Component.text(": "))
                .append(Component.text(String.format("%1.2f", myPet.getExp())).color(NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Translation.getComponent("Name.Type", petOwner))
                .append(Component.text(": "))
                .append(Translation.getComponent("Name." + myPet.getPetType().name(), petOwner).color(NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Translation.getComponent("Name.Skilltree", petOwner))
                .append(Component.text(": "))
                .append(Util.SANITIZED_MINIMESSAGE.deserialize(myPet.getSkilltree() != null ? myPet.getSkilltree().getDisplayName() : "-")
                        .color(NamedTextColor.GOLD));

        HoverEvent<Component> hoverEvent = HoverEvent.showText(hoverBuilder.build());

        petOwner.sendMessage(
                Translation.getComponent("Message.Command.Release.Confirm", petOwner).append(Component.text(" "))
                        .append(
                                myPet.getDisplayName()
                                        .clickEvent(ClickEvent.runCommand("/petrelease " + Util.SANITIZED_MINIMESSAGE.stripTags(myPet.getPetName())))
                                        .hoverEvent(hoverEvent)
                        )
        );
    }
}

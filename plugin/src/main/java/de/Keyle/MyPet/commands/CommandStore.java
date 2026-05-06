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
import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.commands.help.HelpEntry;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.api.entity.StoredPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.locale.Locale;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handles the {@code /petstore} command (aliases: {@code /pstore}, {@code /pst}).
 *
 * <p>Stores (deactivates) the player's currently active pet, making it available for
 * later retrieval via {@code /petswitch}. The pet is unlinked from the current world
 * group so another pet can be activated. Storage is subject to a per-player limit
 * controlled by {@code MyPet.petstorage.limit.<n>} permissions or admin status.</p>
 *
 * <p>This command is restricted to in-game players only (no console support).</p>
 *
 * <p><b>Usage:</b> {@code /petstore}</p>
 *
 * <p><b>Permissions:</b></p>
 * <ul>
 *   <li>{@code MyPet.command.store} -- required to store a pet</li>
 *   <li>{@code MyPet.petstorage.limit.<n>} -- determines the maximum number of stored pets</li>
 *   <li>{@code MyPet.admin} -- grants the maximum configured storage limit</li>
 * </ul>
 */
@SuppressWarnings("UnstableApiUsage")
public class CommandStore {

    /**
     * Registers the {@code /petstore} Brigadier command and its help entry.
     *
     * @param commands     the Paper {@link Commands} registrar used to register the Brigadier command
     * @param helpRegistry the {@link HelpRegistry} to register the command's help entry with
     */
    public void register(Commands commands, HelpRegistry helpRegistry) {
        commands.register(
                Commands.literal("petstore")
                        .requires(ctx -> ctx.getSender() instanceof Player)
                        .executes(ctx -> {
                            execute((Player) ctx.getSource().getSender());
                            return Command.SINGLE_SUCCESS;
                        })
                        .build(),
                "Stores your active pet",
                List.of("pstore", "pst")
        );

        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.Store",
                "/petstore",
                null,
                130,
                player -> MyPetApi.getPetManager().hasActivePet(player)
                        && Permissions.has(player, "MyPet.command.switch")
        ));
    }

    /**
     * Executes the petstore command logic. Validates permissions and storage limits,
     * then deactivates the player's active pet and clears the world group assignment
     * so the slot is freed for another pet.
     *
     * @param player the player executing the command
     */
    private void execute(Player player) {
        if (WorldGroup.getGroupByWorld(player.getWorld()).isDisabled()) {
            player.sendMessage(Locale.getComponent("Message.No.AllowedHere", player));
            return;
        }

        if (!Permissions.has(player, "MyPet.command.store")) {
            player.sendMessage(Locale.getComponent("Message.No.Allowed", player));
            return;
        }

        if (MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
            final MyPetPlayer owner = MyPetApi.getPlayerManager().getMyPetPlayer(player);
            final int maxPetCount = getMaxPetCount(owner.getPlayer());

            if (maxPetCount == 0) {
                player.sendMessage(Locale.getComponent("Message.No.Allowed", player));
                return;
            }

            if (owner.hasPet()) {
                MyPetPlugin.getInstance().getRepository().getPets(owner).thenAccept(pets -> player.getScheduler().run(MyPetApi.getPlugin(), folaTask -> {
                        if (owner.hasPet()) {
                            Pet pet = owner.getPet();
                            String worldGroup = pet.getWorldGroup();

                            int inactivePetCount = getInactivePetCount(pets, worldGroup) - 1; // -1 for active pet
                            if (inactivePetCount >= maxPetCount) {
                                player.sendMessage(Locale.getFormattedComponent("Message.Command.Switch.Limit", player, maxPetCount));
                                return;
                            }
                            if (MyPetApi.getPetManager().deactivatePet(owner, true)) {
                                owner.setPetForWorldGroup(worldGroup, null);
                                player.sendMessage(Locale.getFormattedComponent("Message.Command.Switch.Success", player, pet.getDisplayName()));
                            }
                        } else {
                            player.sendMessage(Locale.getComponent("Message.Command.Switch.NoPet", player));
                        }
                }, null));
                return;
            }
        }
        player.sendMessage(Locale.getComponent("Message.Command.Switch.NoPet", player));
    }

    /**
     * Calculates the maximum number of pets the player is allowed to store.
     * Admins receive {@link Configuration.Misc#MAX_STORED_PET_COUNT}. Other players
     * are checked for {@code MyPet.petstorage.limit.<n>} permissions from highest to lowest.
     *
     * @param p the player to check
     * @return the maximum number of stored pets allowed, or 0 if none
     */
    private int getMaxPetCount(Player p) {
        int maxPetCount = 0;
        if (Permissions.has(p, "MyPet.admin")) {
            maxPetCount = Configuration.Misc.MAX_STORED_PET_COUNT;
        } else {
            for (int i = Configuration.Misc.MAX_STORED_PET_COUNT; i > 0; i--) {
                if (Permissions.has(p, "MyPet.petstorage.limit." + i)) {
                    maxPetCount = i;
                    break;
                }
            }
        }
        return maxPetCount;
    }

    /**
     * Counts the number of pets belonging to the given world group.
     *
     * @param pets       the list of all stored pets for the player
     * @param worldGroup the world group name to filter by
     * @return the number of pets in the specified world group
     */
    private int getInactivePetCount(List<StoredPet> pets, String worldGroup) {
        int inactivePetCount = 0;

        for (StoredPet pet : pets) {
            if (!pet.getWorldGroup().equals(worldGroup)) {
                continue;
            }
            inactivePetCount++;
        }

        return inactivePetCount;
    }
}

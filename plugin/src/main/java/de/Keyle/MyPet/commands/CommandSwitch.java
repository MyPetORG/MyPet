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
import de.Keyle.MyPet.api.entity.StoredPet;
import de.Keyle.MyPet.commands.help.HelpEntry;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.api.gui.MenuId;
import de.Keyle.MyPet.api.gui.MenuIds;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.gui.context.PetSelectionContext;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Handles the {@code /petswitch} command (aliases: {@code /pswitch}, {@code /psw}).
 *
 * <p>Opens a GUI that allows the player to switch between their stored pets. The GUI
 * displays all pets in the current world group with a title showing the current
 * storage usage ({@code inactive/max}). Upon selecting a pet, the currently active pet
 * (if any) is deactivated and the selected pet is activated and spawned.</p>
 *
 * <p>This command is restricted to in-game players only (no console support).</p>
 *
 * <p><b>Usage:</b> {@code /petswitch}</p>
 *
 * <p><b>Permissions:</b></p>
 * <ul>
 *   <li>{@code MyPet.command.switch} -- required to use the switch command</li>
 *   <li>{@code MyPet.petstorage.limit.<n>} -- determines the maximum number of stored pets</li>
 *   <li>{@code MyPet.admin} -- grants the maximum configured storage limit</li>
 * </ul>
 */
public class CommandSwitch {

    /**
     * Registers the {@code /petswitch} Brigadier command and its help entry.
     *
     * @param commands     the Paper {@link Commands} registrar used to register the Brigadier command
     * @param helpRegistry the {@link HelpRegistry} to register the command's help entry with
     */
    public void register(Commands commands, HelpRegistry helpRegistry) {
        commands.register(
                Commands.literal("petswitch")
                        .requires(ctx -> ctx.getSender() instanceof Player)
                        .executes(ctx -> {
                            openSwitchMenu((Player) ctx.getSource().getSender());
                            return Command.SINGLE_SUCCESS;
                        })
                        .build(),
                "Opens the pet switch GUI",
                List.of("pswitch", "psw")
        );

        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.Switch",
                "/petswitch",
                null,
                120,
                player -> MyPetApi.getPetManager().hasActivePet(player)
                        && Permissions.has(player, "MyPet.command.switch")
        ));
    }

    /**
     * Executes the petswitch flow for the given player. Public so the {@code /pet}
     * command can delegate here when the player has no active pet.
     */
    public static void openSwitchMenu(Player player) {
        if (WorldGroup.getGroupByWorld(player.getWorld()).isDisabled()) {
            player.sendMessage(Locale.getComponent("Message.No.AllowedHere", player));
            return;
        }
        if (!Permissions.has(player, "MyPet.command.switch")) {
            player.sendMessage(Locale.getComponent("Message.No.Allowed", player));
            return;
        }

        if (MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
            final MyPetPlayer owner = MyPetApi.getPlayerManager().getMyPetPlayer(player);

            MyPetPlugin.getInstance().getRepository().getPets(owner).thenAccept(pets -> player.getScheduler().run(MyPetApi.getPlugin(), schedTask -> {
                    if (pets.size() - (owner.hasPet() ? 1 : 0) == 0) {
                        owner.sendMessage(Locale.getComponent("Message.Command.Switch.NoStoredPets", owner));
                        return;
                    }
                    if (owner.isOnline()) {
                        String worldGroup = WorldGroup.getGroupByWorld(owner.getPlayer().getWorld().getName()).getName();
                        final UUID activePetUUID = owner.hasPet() ? owner.getPet().getUUID() : null;
                        List<StoredPet> selectablePets = pets.stream()
                                .filter(p -> !p.getWorldGroup().isEmpty() && p.getWorldGroup().equals(worldGroup))
                                .filter(p -> activePetUUID == null || !activePetUUID.equals(p.getUUID()))
                                .collect(Collectors.toList());

                        MyPetApi.getGuiService().openMenu(
                                owner.getPlayer(),
                                (MenuId<PetSelectionContext>) (MenuId<?>) MenuIds.PET_SELECTION,
                                new PetSelectionContext(owner.getPlayer(),
                                        () -> CompletableFuture.completedFuture(selectablePets),
                                        storedPet -> {
                                            Optional<Pet> activePet = MyPetApi.getPetManager().activatePet(storedPet);
                                            if (activePet.isPresent() && owner.isOnline()) {
                                                Player ownerPlayer = owner.getPlayer();
                                                activePet.get().getOwner().sendMessage(Locale.getFormattedComponent("Message.Npc.ChosenPet", owner, activePet.get().getDisplayName()));
                                                WorldGroup wg = WorldGroup.getGroupByWorld(ownerPlayer.getWorld().getName());
                                                owner.setPetForWorldGroup(wg, activePet.get().getUUID());

                                                switch (activePet.get().createEntity()) {
                                                    case Canceled:
                                                        owner.sendMessage(Locale.getFormattedComponent("Message.Spawn.Prevent", owner, activePet.get().getDisplayName()));
                                                        break;
                                                    case NoSpace:
                                                        owner.sendMessage(Locale.getFormattedComponent("Message.Spawn.NoSpace", owner, activePet.get().getDisplayName()));
                                                        break;
                                                    case NotAllowed:
                                                        owner.sendMessage(Locale.getFormattedComponent("Message.No.AllowedHere", owner, activePet.get().getDisplayName()));
                                                        break;
                                                    case Dead:
                                                        if (Configuration.Respawn.DISABLE_AUTO_RESPAWN) {
                                                            owner.sendMessage(Locale.getFormattedComponent("Message.Call.Dead", owner, activePet.get().getDisplayName()));
                                                        } else {
                                                            owner.sendMessage(Locale.getFormattedComponent("Message.Spawn.Respawn.In", owner, activePet.get().getDisplayName(), activePet.get().getRespawnTime()));
                                                        }
                                                        break;
                                                    case Spectator:
                                                        player.sendMessage(Locale.getFormattedComponent("Message.Spawn.Spectator", owner, activePet.get().getDisplayName()));
                                                        break;
                                                }
                                            }
                                        }));
                    }
            }, null));
        } else {
            player.sendMessage(Locale.getComponent("Message.No.HasPet", player));
        }
    }

}

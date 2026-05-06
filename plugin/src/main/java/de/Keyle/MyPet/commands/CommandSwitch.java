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
import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.commands.help.HelpEntry;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.gui.selectionmenu.MyPetSelectionGui;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

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
@SuppressWarnings("UnstableApiUsage")
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
                            execute((Player) ctx.getSource().getSender());
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
                player -> MyPetApi.getPetManager().hasActiveMyPet(player)
                        && Permissions.has(player, "MyPet.command.switch")
        ));
    }

    /**
     * Executes the petswitch command logic. Fetches all stored pets from the repository,
     * opens the {@link MyPetSelectionGui}, and handles the pet activation callback
     * including spawning the selected pet and reporting any spawn failures.
     *
     * @param player the player executing the command
     */
    private void execute(Player player) {
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
                    if (pets.size() - (owner.hasMyPet() ? 1 : 0) == 0) {
                        owner.sendMessage(Locale.getComponent("Message.Command.Switch.NoStoredPets", owner));
                        return;
                    }
                    if (owner.isOnline()) {
                        String worldGroup = WorldGroup.getGroupByWorld(owner.getPlayer().getWorld().getName()).getName();
                        int inactivePetCount = getInactivePetCount(pets, worldGroup);
                        int maxPetCount = getMaxPetCount(owner.getPlayer());

                        Component title;
                        if (owner.hasMyPet()) {
                            inactivePetCount--;
                            title = Locale.getComponent("Message.Npc.SwitchTitle", owner);
                        } else {
                            title = Locale.getComponent("Message.SelectMyPet", owner);
                        }

                        Component stats = Component.text(" (" + inactivePetCount + "/" + maxPetCount + ")");

                        final MyPetSelectionGui gui = new MyPetSelectionGui(owner, title.append(stats), 1);
                        gui.open(pets, storedMyPet -> {
                                Optional<MyPet> activePet = MyPetApi.getPetManager().activateMyPet(storedMyPet);
                                if (activePet.isPresent() && owner.isOnline()) {
                                    Player ownerPlayer = owner.getPlayer();
                                    activePet.get().getOwner().sendMessage(Locale.getFormattedComponent("Message.Npc.ChosenPet", owner, activePet.get().getDisplayName()));
                                    WorldGroup wg = WorldGroup.getGroupByWorld(ownerPlayer.getWorld().getName());
                                    owner.setMyPetForWorldGroup(wg, activePet.get().getUUID());

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
                        });
                    }
            }, null));
        } else {
            player.sendMessage(Locale.getComponent("Message.No.HasPet", player));
        }
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
    private int getInactivePetCount(List<StoredMyPet> pets, String worldGroup) {
        int inactivePetCount = 0;

        for (StoredMyPet pet : pets) {
            if (!pet.getWorldGroup().equals(worldGroup)) {
                continue;
            }
            inactivePetCount++;
        }

        return inactivePetCount;
    }
}

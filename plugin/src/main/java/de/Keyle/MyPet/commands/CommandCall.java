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
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.commands.CommandCategory;
import de.Keyle.MyPet.api.commands.HelpEntry;
import de.Keyle.MyPet.api.commands.HelpRegistry;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.util.MessageUtil;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handles the {@code /petcall} command (aliases {@code /petc}, {@code /pc}) using
 * Paper's Brigadier API.
 *
 * <p>This command spawns (or re-spawns) the player's active pet entity in the world. If
 * the pet entity already exists, it is first removed and then recreated at the player's
 * current location. The command handles a variety of spawn outcomes including success,
 * cancellation by another plugin, insufficient space, disallowed world/region, death
 * with respawn timer, flying, and spectator mode.</p>
 *
 * <h3>Command tree</h3>
 * <pre>
 *   /petcall  - spawn or respawn the player's active pet
 * </pre>
 *
 * <h3>Aliases</h3>
 * <ul>
 *   <li>{@code /petc}</li>
 *   <li>{@code /pc}</li>
 * </ul>
 */
@SuppressWarnings("UnstableApiUsage")
public class CommandCall {

    /**
     * Registers the {@code /petcall} Brigadier command and its help entry.
     *
     * <p>The tree is a single literal {@code petcall} restricted to players. A
     * {@link HelpEntry} is registered under the {@link CommandCategory#PET} category,
     * visible only when the player has an active pet.</p>
     *
     * @param commands     the Paper {@link Commands} registrar used to register the Brigadier command
     * @param helpRegistry the {@link HelpRegistry} to register the command's help entry with
     */
    public void register(Commands commands, HelpRegistry helpRegistry) {
        commands.register(
                Commands.literal("petcall")
                        .requires(ctx -> ctx.getSender() instanceof Player)
                        .executes(ctx -> {
                            execute((Player) ctx.getSource().getSender());
                            return Command.SINGLE_SUCCESS;
                        })
                        .build(),
                "Calls your pet",
                List.of("petc", "pc")
        );

        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.Call",
                "/petcall",
                CommandCategory.PET,
                60,
                player -> MyPetApi.getMyPetManager().hasActiveMyPet(player)
        ));
    }

    /**
     * Executes the pet-call logic for the given player.
     *
     * <p>If the player has an active pet whose entity is already present in the world, the
     * existing entity is removed first (allowing re-teleport to the player). The method
     * then attempts to create the pet entity and sends an appropriate feedback message
     * based on the {@link MyPet.SpawnFlags} result:</p>
     * <ul>
     *   <li>{@code Success} -- pet spawned successfully</li>
     *   <li>{@code Canceled} -- spawn was prevented by another plugin</li>
     *   <li>{@code NoSpace} -- not enough room to spawn the entity</li>
     *   <li>{@code NotAllowed} -- the world or region does not allow pets</li>
     *   <li>{@code Dead} -- the pet is dead and waiting to respawn</li>
     *   <li>{@code Flying} -- the player is flying (mid-air spawn not allowed)</li>
     *   <li>{@code Spectator} -- the player is in spectator mode</li>
     * </ul>
     *
     * @param petOwner the player who issued the command
     */
    private void execute(Player petOwner) {
        if (WorldGroup.getGroupByWorld(petOwner.getWorld()).isDisabled()) {
            petOwner.sendMessage(Translation.getComponent("Message.No.AllowedHere", petOwner));
            return;
        }
        if (MyPetApi.getMyPetManager().hasActiveMyPet(petOwner)) {
            MyPet myPet = MyPetApi.getMyPetManager().getMyPet(petOwner);

            if (myPet.getEntity().isPresent()) {
                //Only let it respawn if it actually was there before
                myPet.removePet(true);
            }

            switch (myPet.createEntity()) {
                case Success:
                    petOwner.sendMessage(MessageUtil.success(
                            Translation.getFormattedComponent(
                                    "Message.Command.Call.Success",
                                    petOwner,
                                    myPet.getDisplayName()
                            ), false
                    ));
                    break;
                case Canceled:
                    petOwner.sendMessage(MessageUtil.error(
                            Translation.getFormattedComponent(
                                    "Message.Spawn.Prevent",
                                    petOwner,
                                    myPet.getDisplayName()
                            ), false
                    ));
                    break;
                case NoSpace:
                    petOwner.sendMessage(MessageUtil.error(
                            Translation.getFormattedComponent(
                                    "Message.Spawn.NoSpace",
                                    petOwner,
                                    myPet.getDisplayName()
                            ), false
                    ));
                    break;
                case NotAllowed:
                    petOwner.sendMessage(MessageUtil.error(
                            Translation.getFormattedComponent(
                                    "Message.No.AllowedHere",
                                    petOwner,
                                    myPet.getDisplayName()
                            ), false
                    ));
                    break;
                case Dead:
                    if (Configuration.Respawn.DISABLE_AUTO_RESPAWN) {
                        petOwner.sendMessage(MessageUtil.info(
                                Translation.getFormattedComponent(
                                        "Message.Call.Dead",
                                        petOwner,
                                        myPet.getDisplayName()
                                ), false
                        ));
                    } else {
                        petOwner.sendMessage(MessageUtil.info(
                                Translation.getFormattedComponent(
                                        "Message.Call.Dead.Respawn",
                                        petOwner,
                                        myPet.getDisplayName(),
                                        myPet.getRespawnTime()
                                ), false
                        ));
                    }
                    break;
                case Flying:
                    petOwner.sendMessage(MessageUtil.error(
                            Translation.getFormattedComponent(
                                    "Message.Spawn.Flying",
                                    petOwner,
                                    myPet.getDisplayName()
                            ), false
                    ));
                    break;
                case Spectator:
                    petOwner.sendMessage(MessageUtil.error(
                            Translation.getFormattedComponent(
                                    "Message.Spawn.Spectator",
                                    petOwner,
                                    myPet.getDisplayName()
                            ), false
                    ));
                    break;
            }
        } else {
            petOwner.sendMessage(Translation.getComponent("Message.No.HasPet", petOwner));
        }
    }
}

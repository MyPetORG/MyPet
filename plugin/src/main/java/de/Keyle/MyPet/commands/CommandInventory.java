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
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.commands.help.CommandCategory;
import de.Keyle.MyPet.commands.help.HelpEntry;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPet.PetState;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.skill.skills.BackpackImpl;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handles the {@code /petinventory} command (aliases: {@code /peti}, {@code /pi}).
 *
 * <p>Opens the Backpack skill inventory for the sender's active pet. Admins can also
 * open another player's pet inventory by specifying a target player name. The pet
 * must be spawned (not dead or despawned) and the player must have the extended
 * inventory permission or admin permission.</p>
 *
 * <p>This command is restricted to in-game players only (no console support).</p>
 *
 * <p><b>Usage:</b> {@code /petinventory [player]}</p>
 *
 * <p><b>Permissions:</b></p>
 * <ul>
 *   <li>{@code MyPet.extended.inventory} -- required to open own pet's inventory</li>
 *   <li>{@code MyPet.admin} -- required to open another player's pet inventory;
 *       also bypasses the extended inventory permission check</li>
 * </ul>
 */
@SuppressWarnings("UnstableApiUsage")
public class CommandInventory {
    /**
     * Registers the {@code /petinventory} Brigadier command and its help entry.
     *
     * <p>The player argument tab-completion is restricted to admins only.</p>
     *
     * @param commands     the Paper {@link Commands} registrar used to register the Brigadier command
     * @param helpRegistry the {@link HelpRegistry} to register the command's help entry with
     */
    public void register(Commands commands, HelpRegistry helpRegistry) {
        commands.register(
                Commands.literal("petinventory")
                        .requires(ctx -> ctx.getSender() instanceof Player)
                        .executes(ctx -> {
                            executeOwn((Player) ctx.getSource().getSender());
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    if (ctx.getSource().getSender() instanceof Player player
                                            && Permissions.has(player, "MyPet.admin")) {
                                        Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    executeOther((Player) ctx.getSource().getSender(), StringArgumentType.getString(ctx, "player"));
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .build(),
                "Opens your pet's inventory",
                List.of("peti", "pi")
        );

        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.Inventory",
                "/petinventory",
                CommandCategory.SKILLS,
                170,
                player -> MyPetApi.getPetManager().hasActiveMyPet(player)
                        && MyPetApi.getPetManager().getMyPet(player).getSkills().isActive(BackpackImpl.class)
        ));
    }

    /**
     * Opens the sender's own pet Backpack inventory. Validates that the pet is alive
     * and spawned, and that the player has the required permission.
     *
     * @param player the player whose pet inventory to open
     */
    private void executeOwn(Player player) {
        if (WorldGroup.getGroupByWorld(player.getWorld()).isDisabled()) {
            player.sendMessage(Locale.getComponent("Message.No.AllowedHere", player));
            return;
        }
        if (MyPetApi.getPetManager().hasActiveMyPet(player)) {
            MyPet myPet = MyPetApi.getPetManager().getMyPet(player);
            if (myPet.getStatus() == PetState.Despawned) {
                player.sendMessage(Locale.getFormattedComponent("Message.Call.First", player, myPet.getDisplayName()));
                return;
            }
            if (myPet.getStatus() == PetState.Dead) {
                player.sendMessage(Locale.getFormattedComponent("Message.Action.Dead", player, myPet.getDisplayName()));
                return;
            }
            if (!Permissions.hasExtended(player, "MyPet.extended.inventory") && !Permissions.has(player, "MyPet.admin")) {
                myPet.getOwner().sendMessage(Locale.getComponent("Message.No.CanUse", player));
                return;
            }
            if (myPet.getSkills().has(BackpackImpl.class)) {
                myPet.getSkills().get(BackpackImpl.class).activate();
            }
        } else {
            player.sendMessage(Locale.getComponent("Message.No.HasPet", player));
        }
    }

    /**
     * Opens another player's pet Backpack inventory. Requires the {@code MyPet.admin}
     * permission; non-admins are redirected to {@link #executeOwn(Player)}.
     *
     * @param player     the admin player executing the command
     * @param targetName the name of the target player whose pet inventory to open
     */
    private void executeOther(Player player, String targetName) {
        if (WorldGroup.getGroupByWorld(player.getWorld()).isDisabled()) {
            player.sendMessage(Locale.getComponent("Message.No.AllowedHere", player));
            return;
        }
        if (!Permissions.has(player, "MyPet.admin")) {
            // Non-admins fall back to own inventory
            executeOwn(player);
            return;
        }
        Player petOwner = Bukkit.getServer().getOfflinePlayer(targetName).getPlayer();
        if (petOwner == null) {
            player.sendMessage(Locale.getComponent("Message.No.PlayerOnline", player));
        } else if (MyPetApi.getPetManager().hasActiveMyPet(petOwner)) {
            MyPet myPet = MyPetApi.getPetManager().getMyPet(petOwner);
            if (myPet.getSkills().isActive(BackpackImpl.class)) {
                myPet.getSkills().get(BackpackImpl.class).openInventory(player);
            }
        }
    }
}

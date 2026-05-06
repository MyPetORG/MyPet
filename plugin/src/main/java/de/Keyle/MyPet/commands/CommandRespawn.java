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
import com.mojang.brigadier.arguments.IntegerArgumentType;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.commands.help.CommandCategory;
import de.Keyle.MyPet.commands.help.HelpEntry;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPet.PetState;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.locale.Locale;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handles the {@code /petrespawn} command (aliases: {@code /pr}, {@code /petr}).
 *
 * <p>Manages pet respawn settings, allowing players to pay to instantly respawn a dead
 * pet, toggle automatic respawn payments, or set a minimum respawn time threshold for
 * the auto-respawn feature. Requires an economy plugin to be enabled (via Vault).</p>
 *
 * <p>This command is restricted to in-game players only (no console support).</p>
 *
 * <p><b>Usage:</b></p>
 * <ul>
 *   <li>{@code /petrespawn} or {@code /petrespawn show} -- display current respawn cost and auto-respawn status</li>
 *   <li>{@code /petrespawn pay} -- pay to instantly respawn a dead pet</li>
 *   <li>{@code /petrespawn auto} -- toggle automatic respawn on/off</li>
 *   <li>{@code /petrespawn auto <min>} -- set the minimum respawn time (in seconds) before auto-respawn triggers</li>
 * </ul>
 *
 * <p><b>Permissions:</b></p>
 * <ul>
 *   <li>{@code MyPet.command.respawn} -- required to use any respawn subcommand</li>
 * </ul>
 */
@SuppressWarnings("UnstableApiUsage")
public class CommandRespawn {
    /**
     * Registers the {@code /petrespawn} Brigadier command and its help entry.
     *
     * @param commands     the Paper {@link Commands} registrar used to register the Brigadier command
     * @param helpRegistry the {@link HelpRegistry} to register the command's help entry with
     */
    public void register(Commands commands, HelpRegistry helpRegistry) {
        commands.register(
                Commands.literal("petrespawn")
                        .requires(ctx -> ctx.getSender() instanceof Player)
                        .executes(ctx -> {
                            Player player = (Player) ctx.getSource().getSender();
                            executeShow(player);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.literal("pay")
                                .executes(ctx -> {
                                    Player player = (Player) ctx.getSource().getSender();
                                    executePay(player);
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .then(Commands.literal("show")
                                .executes(ctx -> {
                                    Player player = (Player) ctx.getSource().getSender();
                                    executeShow(player);
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .then(Commands.literal("auto")
                                .executes(ctx -> {
                                    Player player = (Player) ctx.getSource().getSender();
                                    executeAutoToggle(player);
                                    return Command.SINGLE_SUCCESS;
                                })
                                .then(Commands.argument("min", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            Player player = (Player) ctx.getSource().getSender();
                                            int min = IntegerArgumentType.getInteger(ctx, "min");
                                            executeAutoMin(player, min);
                                            return Command.SINGLE_SUCCESS;
                                        })))
                        .build(),
                "Manages pet respawn settings",
                List.of("pr", "petr")
        );

        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.Respawn",
                "/petrespawn",
                CommandCategory.PET,
                110,
                player -> MyPetApi.getPetManager().hasActiveMyPet(player)
                        && Permissions.has(player, "MyPet.command.respawn")
        ));
    }

    /**
     * Displays the current respawn cost for the player's dead pet and the auto-respawn
     * status (enabled/disabled). If the pet is not dead, the cost is shown as "-".
     *
     * @param petOwner the player executing the command
     */
    private void executeShow(Player petOwner) {
        if (WorldGroup.getGroupByWorld(petOwner.getWorld()).isDisabled()) {
            petOwner.sendMessage(Locale.getComponent("Message.No.AllowedHere", petOwner));
            return;
        }
        if (!MyPetApi.getPetManager().hasActiveMyPet(petOwner)) {
            petOwner.sendMessage(Locale.getComponent("Message.No.HasPet", petOwner));
            return;
        }

        MyPet myPet = MyPetApi.getPetManager().getMyPet(petOwner);
        if (!MyPetApi.getHookHelper().isEconomyEnabled() || !Permissions.has(petOwner, "MyPet.command.respawn")) {
            myPet.getOwner().sendMessage(Locale.getComponent("Message.No.CanUse", petOwner));
            return;
        }

        double costs = myPet.getRespawnTime() * Configuration.Respawn.COSTS_FACTOR + Configuration.Respawn.COSTS_FIXED;
        String costsString;
        if (myPet.getStatus() != PetState.Dead) {
            costsString = "-";
        } else {
            costsString = costs + " " + MyPetApi.getHookHelper().getEconomy().currencyNameSingular();
        }
        myPet.getOwner().sendMessage(Locale.getFormattedComponent("Message.Command.Respawn.Show", petOwner, myPet.getDisplayName(), costsString, Component.text("auto").color(myPet.getOwner().hasAutoRespawnEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED)));
        myPet.getOwner().sendMessage(Locale.getComponent("Message.Command.Respawn.Show.Pay", petOwner));
    }

    /**
     * Attempts to pay the respawn cost to instantly respawn the player's dead pet.
     * The cost is calculated as {@code respawnTime * COSTS_FACTOR + COSTS_FIXED}.
     * If the player has insufficient funds, a "no money" message is sent.
     *
     * @param petOwner the player executing the command
     */
    private void executePay(Player petOwner) {
        if (WorldGroup.getGroupByWorld(petOwner.getWorld()).isDisabled()) {
            petOwner.sendMessage(Locale.getComponent("Message.No.AllowedHere", petOwner));
            return;
        }
        if (!MyPetApi.getPetManager().hasActiveMyPet(petOwner)) {
            petOwner.sendMessage(Locale.getComponent("Message.No.HasPet", petOwner));
            return;
        }

        MyPet myPet = MyPetApi.getPetManager().getMyPet(petOwner);
        if (!MyPetApi.getHookHelper().isEconomyEnabled() || !Permissions.has(petOwner, "MyPet.command.respawn")) {
            myPet.getOwner().sendMessage(Locale.getComponent("Message.No.CanUse", petOwner));
            return;
        }

        double costs = myPet.getRespawnTime() * Configuration.Respawn.COSTS_FACTOR + Configuration.Respawn.COSTS_FIXED;
        if (myPet.getStatus() == PetState.Dead) {
            if (MyPetApi.getHookHelper().getEconomy().canPay(myPet.getOwner(), costs)) {
                MyPetApi.getHookHelper().getEconomy().pay(myPet.getOwner(), costs);
                myPet.getOwner().sendMessage(Locale.getFormattedComponent("Message.Command.Respawn.Paid", petOwner, myPet.getDisplayName(), costs + " " + MyPetApi.getHookHelper().getEconomy().currencyNameSingular()));
                myPet.setRespawnTime(0);
            } else {
                myPet.getOwner().sendMessage(Locale.getFormattedComponent("Message.Command.Respawn.NoMoney", petOwner, myPet.getDisplayName(), costs + " " + MyPetApi.getHookHelper().getEconomy().currencyNameSingular()));
            }
        } else {
            myPet.getOwner().sendMessage(Locale.getComponent("Message.No.CanUse", petOwner));
        }
    }

    /**
     * Toggles the auto-respawn feature on or off for the player. When enabled, the
     * pet will automatically respawn (at cost) when the respawn timer reaches the
     * configured minimum threshold.
     *
     * @param petOwner the player executing the command
     */
    private void executeAutoToggle(Player petOwner) {
        if (WorldGroup.getGroupByWorld(petOwner.getWorld()).isDisabled()) {
            petOwner.sendMessage(Locale.getComponent("Message.No.AllowedHere", petOwner));
            return;
        }
        if (!MyPetApi.getPetManager().hasActiveMyPet(petOwner)) {
            petOwner.sendMessage(Locale.getComponent("Message.No.HasPet", petOwner));
            return;
        }

        MyPet myPet = MyPetApi.getPetManager().getMyPet(petOwner);
        if (!MyPetApi.getHookHelper().isEconomyEnabled() || !Permissions.has(petOwner, "MyPet.command.respawn")) {
            myPet.getOwner().sendMessage(Locale.getComponent("Message.No.CanUse", petOwner));
            return;
        }

        myPet.getOwner().setAutoRespawnEnabled(!myPet.getOwner().hasAutoRespawnEnabled());
        myPet.getOwner().sendMessage(Locale.getFormattedComponent("Message.Command.Respawn.Auto", petOwner, Locale.getComponent("Name." + (myPet.getOwner().hasAutoRespawnEnabled() ? "Enabled" : "Disabled"), petOwner)));
    }

    /**
     * Sets the minimum respawn time threshold (in seconds) for the auto-respawn feature.
     * Auto-respawn will only trigger when the remaining respawn time is at or below
     * this value.
     *
     * @param petOwner the player executing the command
     * @param min      the minimum respawn time in seconds (must be at least 1)
     */
    private void executeAutoMin(Player petOwner, int min) {
        if (WorldGroup.getGroupByWorld(petOwner.getWorld()).isDisabled()) {
            petOwner.sendMessage(Locale.getComponent("Message.No.AllowedHere", petOwner));
            return;
        }
        if (!MyPetApi.getPetManager().hasActiveMyPet(petOwner)) {
            petOwner.sendMessage(Locale.getComponent("Message.No.HasPet", petOwner));
            return;
        }

        MyPet myPet = MyPetApi.getPetManager().getMyPet(petOwner);
        if (!MyPetApi.getHookHelper().isEconomyEnabled() || !Permissions.has(petOwner, "MyPet.command.respawn")) {
            myPet.getOwner().sendMessage(Locale.getComponent("Message.No.CanUse", petOwner));
            return;
        }

        myPet.getOwner().setAutoRespawnMin(min);
        myPet.getOwner().sendMessage(Locale.getFormattedComponent("Message.Command.Respawn.AutoMin", petOwner, String.valueOf(min)));
    }
}

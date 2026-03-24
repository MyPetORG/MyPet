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
import de.Keyle.MyPet.api.commands.CommandCategory;
import de.Keyle.MyPet.api.commands.HelpEntry;
import de.Keyle.MyPet.api.commands.HelpRegistry;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.locale.Translation;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handles the {@code /petcapturehelper} command, which toggles the capture helper overlay
 * for a player. The capture helper displays visual cues (e.g., health bar indicators) on
 * mobs that are eligible to be captured as pets, helping players know when a mob is low
 * enough to leash.
 *
 * <p>The capture helper cannot be enabled while the player already has an active pet.</p>
 *
 * <p><b>Usage:</b> {@code /petcapturehelper}</p>
 * <p><b>Aliases:</b> {@code /pch}</p>
 * <p><b>Permissions:</b> {@code MyPet.command.capturehelper} — required to toggle the helper</p>
 * <p><b>Help category:</b> {@link CommandCategory#PET PET} (priority 180)</p>
 */
@SuppressWarnings("UnstableApiUsage")
public class CommandCaptureHelper {

    /**
     * Registers the {@code /petcapturehelper} Brigadier command and its help entry.
     *
     * <p>The command is a player-only literal with no arguments. The help entry is
     * always visible (no conditional predicate).</p>
     *
     * @param commands     the Paper {@link Commands} registrar used to register the Brigadier command
     * @param helpRegistry the {@link HelpRegistry} to register the command's help entry with
     */
    public void register(Commands commands, HelpRegistry helpRegistry) {
        commands.register(
                Commands.literal("petcapturehelper")
                        .requires(ctx -> ctx.getSender() instanceof Player)
                        .executes(ctx -> {
                            execute((Player) ctx.getSource().getSender());
                            return Command.SINGLE_SUCCESS;
                        })
                        .build(),
                "Toggles the capture helper",
                List.of("pch")
        );

        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.CaptureHelper",
                "/petcapturehelper",
                CommandCategory.PET,
                180,
                null
        ));
    }

    /**
     * Toggles the capture helper for the given player.
     *
     * <p>Checks that the world group is enabled, the player has the required permission,
     * and that the player does not currently have an active pet. If the player is not yet
     * registered as a MyPet player, they are registered automatically. Sends a confirmation
     * message indicating whether the capture helper was enabled or disabled.</p>
     *
     * @param player the player whose capture helper state should be toggled
     */
    private void execute(Player player) {
        if (WorldGroup.getGroupByWorld(player.getWorld()).isDisabled()) {
            player.sendMessage(Translation.getComponent("Message.No.AllowedHere", player));
            return;
        }

        if (Permissions.has(player, "MyPet.command.capturehelper")) {
            MyPetPlayer myPetPlayer;
            if (MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
                myPetPlayer = MyPetApi.getPlayerManager().getMyPetPlayer(player);

                if (myPetPlayer.hasMyPet()) {
                    player.sendMessage(Translation.getComponent("Message.Command.CaptureHelper.HasPet", player));
                    return;
                }
            } else {
                myPetPlayer = MyPetApi.getPlayerManager().registerMyPetPlayer(player);
            }

            myPetPlayer.setCaptureHelperActive(!myPetPlayer.isCaptureHelperActive());
            Component mode = myPetPlayer.isCaptureHelperActive() ? Translation.getComponent("Name.Enabled", player) : Translation.getComponent("Name.Disabled", player);
            player.sendMessage(Translation.getFormattedComponent("Message.Command.CaptureHelper.Mode", player, mode));
            return;
        }
        player.sendMessage(Translation.getComponent("Message.No.Allowed", player));
    }
}

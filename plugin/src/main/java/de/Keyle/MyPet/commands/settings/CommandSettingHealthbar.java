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

package de.Keyle.MyPet.commands.settings;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.locale.Translation;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

/**
 * Provides the {@code /petsettings healthbar} subcommand, which toggles the visibility
 * of the pet's health bar for the executing player.
 *
 * <h3>Usage</h3>
 * <p>{@code /petsettings healthbar}</p>
 *
 * <p>This is a player-only command with no additional permission requirement beyond
 * being a registered MyPet player. Each invocation flips the health bar display
 * state between enabled and disabled.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public class CommandSettingHealthbar {

    /**
     * Builds and returns the Brigadier {@code "healthbar"} literal command node.
     * This node is intended to be attached as a child of the {@code /petsettings} command tree.
     *
     * @return the built {@link LiteralCommandNode} for the healthbar subcommand
     */
    public LiteralCommandNode<CommandSourceStack> buildNode() {
        return Commands.literal("healthbar")
                .requires(ctx -> ctx.getSender() instanceof Player)
                .executes(ctx -> {
                    execute((Player) ctx.getSource().getSender());
                    return Command.SINGLE_SUCCESS;
                })
                .build();
    }

    /**
     * Toggles the health bar display setting for the given player. If the player is a
     * registered MyPet player, their health bar active state is flipped.
     *
     * @param player the player toggling the setting
     */
    private void execute(Player player) {
        if (MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
            MyPetPlayer myPetPlayer = MyPetApi.getPlayerManager().getMyPetPlayer(player);
            myPetPlayer.setHealthBarActive(!myPetPlayer.isHealthBarActive());
            player.sendMessage(Translation.getComponent("Message.Command.Success", player));
        } else {
            player.sendMessage(Translation.getComponent("Message.Command.Fail", player));
        }
    }
}

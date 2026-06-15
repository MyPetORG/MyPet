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

package de.Keyle.MyPet.commands.settings;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.gui.MenuId;
import de.Keyle.MyPet.api.gui.MenuIds;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.gui.context.PetVolumeContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Provides the {@code /petsettings volume} subcommand, which allows players to
 * adjust the volume of all sounds their pet emits.
 *
 * <h3>Usage</h3>
 * <p>{@code /petsettings volume <amount>}</p>
 *
 * <p>The {@code amount} parameter is an integer from 0 to 100, representing the volume
 * percentage. Preset suggestions of 100, 75, 50, 25, and 0 are offered via tab completion.
 * Setting the value to 0 effectively mutes the pet entirely.</p>
 *
 * <p>This is a player-only command with no additional permission requirement beyond
 * being a registered MyPet player.</p>
 */
public class CommandSettingsPetVolume {

    private static final List<Integer> PRESET_VOLUMES = List.of(100, 75, 50, 25, 0);

    /**
     * Builds and returns the Brigadier {@code "volume"} literal command node.
     * This node is intended to be attached as a child of the {@code /petsettings} command tree.
     *
     * @return the built {@link LiteralCommandNode} for the volume subcommand
     */
    @SuppressWarnings("unchecked")
    public LiteralCommandNode<CommandSourceStack> buildNode() {
        return Commands.literal("volume")
                .requires(ctx -> ctx.getSender() instanceof Player)
                .executes(ctx -> {
                    Player player = (Player) ctx.getSource().getSender();
                    if (!MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
                        player.sendMessage(Locale.getComponent("Message.Command.Fail", player));
                        return Command.SINGLE_SUCCESS;
                    }
                    MyPetApi.getGuiService().openMenu(
                            player,
                            (MenuId<PetVolumeContext>) (MenuId<?>) MenuIds.PET_VOLUME,
                            new PetVolumeContext(player)
                    );
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.argument("amount", IntegerArgumentType.integer(0, 100))
                        .suggests((ctx, builder) -> {
                            for (int volume : PRESET_VOLUMES) {
                                builder.suggest(volume);
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            Player player = (Player) ctx.getSource().getSender();
                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                            execute(player, amount);
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
    }

    /**
     * Sets the pet volume for the given player. The amount is clamped to
     * the range [0, 100] and converted to a float ratio (0.0 to 1.0) before being stored.
     *
     * @param player the player adjusting the setting
     * @param amount the desired volume percentage (0-100)
     */
    private void execute(Player player, int amount) {
        if (MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
            float volume = Math.min(Math.max(amount, 0f), 100f) / 100f;
            MyPetPlayer myPetPlayer = MyPetApi.getPlayerManager().getMyPetPlayer(player);
            myPetPlayer.setPetVolume(volume);
            player.sendMessage(Locale.getComponent("Message.Command.Success", player));
        } else {
            player.sendMessage(Locale.getComponent("Message.Command.Fail", player));
        }
    }
}

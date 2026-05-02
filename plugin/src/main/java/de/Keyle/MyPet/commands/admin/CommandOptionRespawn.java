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

package de.Keyle.MyPet.commands.admin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.commands.help.CommandCategory;
import de.Keyle.MyPet.commands.help.HelpEntry;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPet.PetState;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.util.MessageUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Admin subcommand for viewing and modifying a pet's respawn timer.
 *
 * <p>This command provides three modes of operation:
 * <ul>
 *   <li>{@code /petadmin respawn <player>} -- instantly respawns the pet by setting
 *       the respawn timer to 0 seconds (only works if the pet is dead)</li>
 *   <li>{@code /petadmin respawn <player> show} -- displays the pet's current respawn
 *       timer in seconds</li>
 *   <li>{@code /petadmin respawn <player> <time>} -- sets the pet's respawn timer to
 *       the specified number of seconds (only works if the pet is dead)</li>
 * </ul>
 *
 * <p>Requires the {@code MyPet.admin} permission.
 */
@SuppressWarnings("UnstableApiUsage")
public class CommandOptionRespawn {

    /**
     * Builds the Brigadier command node for the {@code respawn} admin subcommand.
     *
     * <p>The resulting command tree structure is:
     * <pre>
     *   respawn
     *     &lt;player: player_selector&gt;
     *       (executes) -- set respawn time to 0 (instant respawn)
     *       show
     *         (executes) -- display current respawn time
     *       &lt;time: integer (min 0)&gt;
     *         (executes) -- set respawn time to the given value
     * </pre>
     *
     * @param helpRegistry the help registry to register the command's help entry with
     * @return the built {@link LiteralCommandNode} representing the {@code respawn} subcommand
     */
    public LiteralCommandNode<CommandSourceStack> buildNode(HelpRegistry helpRegistry) {
        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.Admin.Respawn",
                "/petadmin respawn",
                CommandCategory.ADMIN,
                30,
                player -> Permissions.has(player, "MyPet.admin")
        ));

        return Commands.literal("respawn")
                .then(Commands.argument("player", ArgumentTypes.player())
                        // /petadmin respawn <player> (no second arg - set to 0)
                        .executes(ctx -> {
                            Player player = ctx.getArgument("player", PlayerSelectorArgumentResolver.class)
                                    .resolve(ctx.getSource()).getFirst();
                            executeSet(ctx.getSource().getSender(), player, 0);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.literal("show")
                                .executes(ctx -> {
                                    Player player = ctx.getArgument("player", PlayerSelectorArgumentResolver.class)
                                            .resolve(ctx.getSource()).getFirst();
                                    executeShow(ctx.getSource().getSender(), player);
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .then(Commands.argument("time", IntegerArgumentType.integer(0))
                                .executes(ctx -> {
                                    Player player = ctx.getArgument("player", PlayerSelectorArgumentResolver.class)
                                            .resolve(ctx.getSource()).getFirst();
                                    executeSet(ctx.getSource().getSender(), player,
                                            IntegerArgumentType.getInteger(ctx, "time"));
                                    return Command.SINGLE_SUCCESS;
                                })))
                .build();
    }

    /**
     * Displays the current respawn timer for the specified player's active pet.
     *
     * @param sender   the command sender (admin) to receive the respawn time message
     * @param petOwner the player whose pet's respawn time will be shown
     */
    private void executeShow(CommandSender sender, Player petOwner) {
        String lang = Locale.getCommandSenderLanguage(sender);
        if (!MyPetApi.getMyPetManager().hasActiveMyPet(petOwner)) {
            sender.sendMessage(MessageUtil.prefixed(Locale.getFormattedComponent("Message.No.UserHavePet", lang, petOwner.getName())));
            return;
        }
        MyPet myPet = MyPetApi.getMyPetManager().getMyPet(petOwner);
        sender.sendMessage(MessageUtil.prefixed(Component.text("respawn time: " + myPet.getRespawnTime() + "sec.")));
    }

    /**
     * Sets the respawn timer for the specified player's active pet.
     *
     * <p>The pet must be in the {@link PetState#Dead} state for the timer to be modified.
     * If the pet is alive or despawned, an error message is sent instead.
     *
     * @param sender   the command sender (admin) to receive confirmation or error messages
     * @param petOwner the player whose pet's respawn time will be set
     * @param time     the new respawn time in seconds (0 for instant respawn)
     */
    private void executeSet(CommandSender sender, Player petOwner, int time) {
        String lang = Locale.getCommandSenderLanguage(sender);
        if (!MyPetApi.getMyPetManager().hasActiveMyPet(petOwner)) {
            sender.sendMessage(MessageUtil.prefixed(Locale.getFormattedComponent("Message.No.UserHavePet", lang, petOwner.getName())));
            return;
        }
        MyPet myPet = MyPetApi.getMyPetManager().getMyPet(petOwner);
        if (myPet.getStatus() == PetState.Dead) {
            myPet.setRespawnTime(time);
            sender.sendMessage(MessageUtil.prefixed(Component.text("set respawn time to: " + myPet.getRespawnTime() + "sec.")));
        } else {
            sender.sendMessage(MessageUtil.prefixed(Component.text("pet is not dead!")));
        }
    }
}

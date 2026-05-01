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
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.Keyle.MyPet.api.Configuration.LevelSystem.Experience.Modifier;
import de.Keyle.MyPet.commands.help.CommandCategory;
import de.Keyle.MyPet.commands.help.HelpEntry;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.api.player.Permissions;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

/**
 * Admin subcommand for viewing and modifying the global experience rate multiplier.
 *
 * <p>This command provides two modes of operation under the {@code /petadmin exp-rate global} path:
 * <ul>
 *   <li>{@code /petadmin exp-rate global} -- displays the current global experience rate</li>
 *   <li>{@code /petadmin exp-rate global <rate>} -- sets the global experience rate to the specified value</li>
 * </ul>
 *
 * <p>The global experience rate is a multiplier applied to all pet experience gain server-wide.
 * Changes take effect immediately but are not persisted across server restarts (the value is
 * stored in {@link Modifier#GLOBAL}).
 *
 * <p>Requires the {@code MyPet.admin} permission.
 */
@SuppressWarnings("UnstableApiUsage")
public class CommandOptionExpRate {

    /**
     * Builds the Brigadier command node for the {@code exp-rate} admin subcommand.
     *
     * <p>The resulting command tree structure is:
     * <pre>
     *   exp-rate
     *     global
     *       (executes) -- show current global rate
     *       &lt;rate: double&gt;
     *         (executes) -- set global rate to the given value
     * </pre>
     *
     * <p>Also registers a help entry for {@code /petadmin exp-rate} in the
     * {@link CommandCategory#ADMIN} category.
     *
     * @param helpRegistry the help registry to register the command's help entry with
     * @return the built {@link LiteralCommandNode} representing the {@code exp-rate} subcommand
     */
    public LiteralCommandNode<CommandSourceStack> buildNode(HelpRegistry helpRegistry) {
        helpRegistry.register(new HelpEntry(
                "Message.Command.Help.Admin.ExpRate",
                "/petadmin exp-rate",
                CommandCategory.ADMIN,
                28,
                player -> Permissions.has(player, "MyPet.admin", false)
        ));

        return Commands.literal("exp-rate")
                .then(Commands.literal("global")
                        // /petadmin exp-rate global (show current rate)
                        .executes(ctx -> {
                            ctx.getSource().getSender().sendMessage(
                                    Component.text("Global Exp Rate: ")
                                            .append(Component.text(String.valueOf(Modifier.GLOBAL)).color(NamedTextColor.DARK_AQUA)));
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument("rate", DoubleArgumentType.doubleArg(0))
                                .executes(ctx -> {
                                    CommandSender sender = ctx.getSource().getSender();
                                    Modifier.GLOBAL = DoubleArgumentType.getDouble(ctx, "rate");
                                    sender.sendMessage(
                                            Component.text("Global Exp Rate set to: ")
                                                    .append(Component.text(String.valueOf(Modifier.GLOBAL)).color(NamedTextColor.DARK_AQUA)));
                                    return Command.SINGLE_SUCCESS;
                                })))
                .build();
    }
}

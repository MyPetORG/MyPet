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

package de.Keyle.MyPet.commands.mypet;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.Keyle.MyPet.api.player.AdminPermissions;
import de.Keyle.MyPet.api.util.locale.Locale;
import de.Keyle.MyPet.util.MessageUtil;
import de.Keyle.MyPet.util.MyPetReloader;
import de.Keyle.MyPet.util.shop.ShopManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

/**
 * Provides the {@code /mypet reload} subcommand, enabling hot-reload of plugin resources.
 *
 * <p>This command is restricted to the console and players with the {@code MyPet.admin.reload}
 * permission (or the {@code MyPet.admin} bundle). It supports four reload targets:</p>
 *
 * <h3>Command tree</h3>
 * <pre>
 *   /mypet reload             - shows usage hint (missing parameter)
 *   /mypet reload all         - reloads config, skilltrees, and shops
 *   /mypet reload config      - reloads config.yml, translations, hook configs, and
 *                                recalculates pet-storage permissions and XP calculator
 *   /mypet reload skilltrees  - reloads skilltree JSON files from the skilltrees/
 *                                directory and reassigns trees to all active pets
 *   /mypet reload shops       - reloads shop definitions via the {@link ShopManager}
 * </pre>
 */
public class CommandOptionReload {

    /**
     * Builds the {@code reload} literal command node to be mounted under {@code /mypet}.
     *
     * <p>The node requires {@code MyPet.admin.reload} permission (or console, or the {@code MyPet.admin} bundle) and defines four
     * sub-literals ({@code all}, {@code config}, {@code skilltrees}, {@code shops}). The
     * bare {@code /mypet reload} execution (without a target) sends a usage hint listing
     * the available targets.</p>
     *
     * @return the built {@link LiteralCommandNode} for the {@code reload} subtree
     */
    public LiteralCommandNode<CommandSourceStack> buildNode() {
        return Commands.literal("reload")
                .requires(AdminPermissions.requiresNode(AdminPermissions.RELOAD))
                .then(Commands.literal("all")
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            reloadConfig(sender);
                            reloadSkilltrees(sender);
                            reloadShops(sender);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("config")
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            reloadConfig(sender);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("skilltrees")
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            reloadSkilltrees(sender);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("shops")
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            reloadShops(sender);
                            return Command.SINGLE_SUCCESS;
                        }))
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    sender.sendMessage(Locale.getComponent("Message.Command.Help.MissingParameter", sender));
                    sender.sendMessage(Component.text(" -> ")
                            .append(Component.text("/mypet reload ").color(NamedTextColor.DARK_AQUA))
                            .append(Component.text("<all|config|shops|skilltrees>").color(NamedTextColor.RED)));
                    return Command.SINGLE_SUCCESS;
                })
                .build();
    }

    /**
     * Reloads the config and confirms to {@code sender}. See {@code MyPetReloader#reloadConfig}
     * for what that covers.
     */
    protected void reloadConfig(CommandSender sender) {
        MyPetReloader.reloadConfig();
        message(sender, "config reloaded!");
    }

    /**
     * Reloads the skilltrees and confirms to {@code sender}. See
     * {@code MyPetReloader#reloadSkilltrees} for what that covers.
     */
    protected void reloadSkilltrees(CommandSender sender) {
        MyPetReloader.reloadSkilltrees();
        message(sender, "skilltrees reloaded!");
    }

    /**
     * Reloads the shops and confirms to {@code sender}. See {@code MyPetReloader#reloadShops}
     * for what that covers.
     */
    protected void reloadShops(CommandSender sender) {
        MyPetReloader.reloadShops();
        message(sender, "shops reloaded!");
    }

    /** Confirm to the sender. Console already sees MyPetReloader's log line, so skip the chat echo. */
    private static void message(CommandSender sender, String text) {
        if (!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(MessageUtil.prefixed(Component.text(text)));
        }
    }
}

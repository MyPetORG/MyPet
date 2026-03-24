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

package de.Keyle.MyPet.commands.mypet;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.Keyle.MyPet.api.MyPetVersion;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.util.Updater;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/**
 * Provides the {@code /mypet update} subcommand, which checks whether a newer version
 * of the MyPet plugin is available.
 *
 * <h3>Usage</h3>
 * <p>{@code /mypet update}</p>
 *
 * <h3>Permissions</h3>
 * <ul>
 *   <li>{@code MyPet.admin} -- required for players; console can always execute</li>
 * </ul>
 *
 * <p>Reports the latest available version if an update exists, or confirms the
 * installation is up to date. For local (development) builds, update checks are skipped.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public class CommandOptionUpdate {

    /**
     * Builds and returns the Brigadier {@code "update"} literal command node.
     * This node is intended to be attached as a child of the {@code /mypet} command tree.
     *
     * @return the built {@link LiteralCommandNode} for the update subcommand
     */
    public LiteralCommandNode<CommandSourceStack> buildNode() {
        return Commands.literal("update")
                .requires(ctx -> {
                    var sender = ctx.getSender();
                    return !(sender instanceof Player p) || Permissions.has(p, "MyPet.admin", false);
                })
                .executes(ctx -> {
                    var sender = ctx.getSource().getSender();
                    if (Updater.isUpdateAvailable()) {
                        sender.sendMessage(Component.text("A new version is available: ")
                                .append(Component.text(Updater.getLatest().toString()).color(NamedTextColor.GOLD)));
                    } else if (MyPetVersion.isLocalBuild()) {
                        sender.sendMessage(Component.text("You are running a ")
                                .append(Component.text("local build").color(NamedTextColor.YELLOW))
                                .append(Component.text(". Update checks are skipped.")));
                    } else {
                        sender.sendMessage("Your version of MyPet is up to date.");
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .build();
    }
}

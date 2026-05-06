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

package de.Keyle.MyPet.commands.admin;

import com.mojang.brigadier.tree.LiteralCommandNode;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.commands.admin.npc.CommandOptionShop;
import de.Keyle.MyPet.commands.admin.npc.CommandOptionWallet;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

/**
 * Provides the {@code /petadmin npc} subcommand group, which contains admin commands
 * for managing Citizens NPC traits related to MyPet.
 *
 * <h3>Usage</h3>
 * <ul>
 *   <li>{@code /petadmin npc shop <shopname>} -- assign a pet shop to the selected NPC</li>
 *   <li>{@code /petadmin npc wallet <type> [account]} -- configure the wallet trait on the selected NPC</li>
 * </ul>
 *
 * <p>This subcommand group is only available when the Citizens plugin hook is active.
 * Individual subcommands are delegated to {@link CommandOptionShop} and {@link CommandOptionWallet}.</p>
 */
public class CommandOptionNpc {

    /**
     * Builds and returns the Brigadier {@code "npc"} literal command node, which serves
     * as a parent for the {@code shop} and {@code wallet} subcommands. The node requires
     * the Citizens plugin hook to be active.
     *
     * @param helpRegistry the help registry to register the command's help entry with
     * @return the built {@link LiteralCommandNode} representing the {@code npc} subcommand
     */
    public LiteralCommandNode<CommandSourceStack> buildNode(HelpRegistry helpRegistry) {
        return Commands.literal("npc")
                .requires(ctx -> MyPetApi.getPluginHookManager().isHookActive("Citizens"))
                .then(new CommandOptionShop().buildNode(helpRegistry))
                .then(new CommandOptionWallet().buildNode(helpRegistry))
                .build();
    }
}

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

import de.Keyle.MyPet.commands.help.HelpRegistry;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.commands.admin.*;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handles the {@code /mypetadmin} command (alias {@code /petadmin}) using Paper's
 * Brigadier API.
 *
 * <p>This is the administrative top-level command. It requires the {@code MyPet.admin}
 * permission (or console access) and delegates to a set of admin subcommand nodes, each
 * provided by a dedicated class in the {@code de.Keyle.MyPet.commands.admin} package.</p>
 *
 * <h3>Command tree</h3>
 * <pre>
 *   /mypetadmin name        - rename another player's pet
 *   /mypetadmin exp         - modify a pet's experience points
 *   /mypetadmin exprate     - view or change the global XP rate multiplier
 *   /mypetadmin respawn     - force-respawn a pet or adjust its respawn timer
 *   /mypetadmin skilltree   - assign a skilltree to a pet
 *   /mypetadmin create      - create a new pet for a player
 *   /mypetadmin clone       - clone a pet from one player to another
 *   /mypetadmin remove      - permanently remove a player's pet
 *   /mypetadmin purge       - purge inactive pet data
 *   /mypetadmin switch      - switch a player's active pet
 *   /mypetadmin info        - display detailed info about a player's pet
 *   /mypetadmin npc         - manage NPC-related pet features
 * </pre>
 *
 * <h3>Aliases</h3>
 * <ul>
 *   <li>{@code /petadmin}</li>
 * </ul>
 */
@SuppressWarnings("UnstableApiUsage")
public class CommandAdmin {

    /**
     * Registers the {@code /mypetadmin} Brigadier command and its help entry.
     *
     * <p>The root literal {@code mypetadmin} requires the sender to either be the console
     * or a player with the {@code MyPet.admin} permission. Each admin subcommand is
     * mounted as a child literal node built by its respective command option class, which
     * also registers its own {@link HelpEntry} in the shared {@link HelpRegistry}.</p>
     *
     * @param commands     the Paper {@link Commands} registrar used to register the Brigadier command
     * @param helpRegistry the {@link HelpRegistry} to register the command's help entry with
     */
    public void register(Commands commands, HelpRegistry helpRegistry) {
        commands.register(
                Commands.literal("mypetadmin")
                        .requires(ctx -> !(ctx.getSender() instanceof Player player)
                                || Permissions.has(player, "MyPet.admin"))
                        .then(new CommandOptionName().buildNode(helpRegistry))
                        .then(new CommandOptionExp().buildNode(helpRegistry))
                        .then(new CommandOptionExpRate().buildNode(helpRegistry))
                        .then(new CommandOptionRespawn().buildNode(helpRegistry))
                        .then(new CommandOptionSkilltree().buildNode(helpRegistry))
                        .then(new CommandOptionCreate().buildNode(helpRegistry))
                        .then(new CommandOptionClone().buildNode(helpRegistry))
                        .then(new CommandOptionRemove().buildNode(helpRegistry))
                        .then(new CommandOptionPurge().buildNode(helpRegistry))
                        .then(new CommandOptionSwitch().buildNode(helpRegistry))
                        .then(new CommandOptionInfo().buildNode(helpRegistry))
                        .then(new CommandOptionNpc().buildNode(helpRegistry))
                        .build(),
                "MyPet admin commands",
                List.of("petadmin")
        );
    }
}

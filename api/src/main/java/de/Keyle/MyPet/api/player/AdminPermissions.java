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

package de.Keyle.MyPet.api.player;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import java.util.List;
import java.util.function.Predicate;

/** Single source of truth for MyPet admin permission node strings + parent-gate helper. */
public final class AdminPermissions {
    // /mypet admin subcommands
    public static final String EDITOR = "MyPet.admin.editor";
    public static final String RELOAD = "MyPet.admin.reload";
    public static final String TICKET = "MyPet.admin.ticket";
    public static final String UPDATE = "MyPet.admin.update";
    // /mypetadmin subcommands
    public static final String CLONE = "MyPet.admin.clone";
    public static final String CREATE = "MyPet.admin.create";
    public static final String EXP = "MyPet.admin.exp";
    public static final String EXPRATE = "MyPet.admin.exprate";
    public static final String INFO = "MyPet.admin.info";
    public static final String NAME = "MyPet.admin.name";
    public static final String NPC = "MyPet.admin.npc";
    public static final String PURGE = "MyPet.admin.purge";
    public static final String REMOVE = "MyPet.admin.remove";
    public static final String RESPAWN = "MyPet.admin.respawn";
    public static final String SKILLTREE = "MyPet.admin.skilltree";
    public static final String SWITCH = "MyPet.admin.switch";
    // act-on-others (command family)
    public static final String INFO_OTHER = "MyPet.command.info.other";
    public static final String LIST_OTHER = "MyPet.command.list.other";
    public static final String SKILL_OTHER = "MyPet.command.skill.other";
    public static final String INVENTORY_OTHER = "MyPet.command.inventory.other";
    public static final String SENDAWAY_OTHER = "MyPet.command.sendaway.other";
    // bypass nodes (exempt from a player-facing restriction)
    public static final String BYPASS_SKILLTREE = "MyPet.bypass.skilltree";
    public static final String BYPASS_FEE = "MyPet.bypass.fee";
    public static final String BYPASS_DEATH = "MyPet.bypass.death";
    public static final String BYPASS_INVENTORY = "MyPet.bypass.inventory";
    public static final String BYPASS_CREATIVE = "MyPet.bypass.creative";
    // unlimited-storage tier in the existing MyPet.petstorage.limit.<n> family
    public static final String PETSTORAGE_LIMIT_ALL = "MyPet.petstorage.limit.*";

    /** The /mypetadmin subcommand nodes — used to gate the /mypetadmin parent literal. */
    public static final List<String> MYPETADMIN_NODES = List.of(
            CLONE, CREATE, EXP, EXPRATE, INFO, NAME, NPC, PURGE, REMOVE, RESPAWN, SKILLTREE, SWITCH);

    /** True if the player holds any node in the set (a MyPet.admin holder has them all via children). */
    public static boolean hasAnyOf(Player player, List<String> nodes) {
        for (String node : nodes) {
            if (Permissions.has(player, node)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A Brigadier {@code .requires()} predicate that admits non-player senders
     * (console, command blocks) unconditionally and gates players on {@code node}.
     * Single-sources the per-command admin gate so every subcommand wires it identically.
     */
    public static Predicate<CommandSourceStack> requiresNode(String node) {
        return ctx -> !(ctx.getSender() instanceof Player p) || Permissions.has(p, node);
    }

    private AdminPermissions() {
    }
}

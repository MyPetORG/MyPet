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

import de.Keyle.MyPet.api.Configuration;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;

/**
 * Static permission-check utility. All MyPet permission queries go through
 * this class rather than calling {@code player.hasPermission()} directly,
 * because MyPet supports an "extended permissions" mode
 * ({@link Configuration.Permissions#EXTENDED}) that gates optional features
 * behind additional permission nodes.
 * <p>
 * Two families of checks:
 * <ul>
 *   <li>{@code has()} — unconditional check. Returns {@code true} if the
 *       player is OP or has the node.</li>
 *   <li>{@code hasExtended()} — conditional check. Only evaluates the node
 *       when extended-permissions mode is enabled; otherwise returns
 *       {@code true} (or the supplied default). This lets admins run MyPet
 *       with minimal permission setup when extended mode is off.</li>
 * </ul>
 */
public class Permissions {

    /**
     * Checks a permission node against a MyPetPlayer. Returns
     * {@code false} if the player is null or offline.
     */
    public static boolean has(MyPetPlayer player, String node) {
        if (player != null && player.isOnline()) {
            return has(player.getPlayer(), node);
        }
        return false;
    }

    /**
     * Checks a permission node against an online player. OP always
     * passes.
     */
    public static boolean has(Player player, String node) {
        if (player != null) {
            return player.isOp() || player.hasPermission(node);
        }
        return false;
    }

    /**
     * Extended-mode check for a MyPetPlayer. When extended permissions
     * are disabled globally, returns {@code true} without checking the
     * node.
     */
    public static boolean hasExtended(MyPetPlayer player, String node) {
        if (player != null && player.isOnline()) {
            return hasExtended(player.getPlayer(), node);
        }
        return !Configuration.Permissions.EXTENDED;
    }

    /**
     * Extended-mode check. Returns {@code true} if extended permissions
     * are disabled, or if the player has the node.
     */
    public static boolean hasExtended(Player player, String node) {
        return !Configuration.Permissions.EXTENDED || has(player, node);
    }

    /**
     * Extended-mode check with an explicit default. When extended
     * permissions are disabled, returns {@code defaultValue} instead
     * of unconditionally passing.
     */
    public static boolean hasExtended(Player player, String node, boolean defaultValue) {
        if (Configuration.Permissions.EXTENDED) {
            return has(player, node);
        }
        return defaultValue;
    }

    /**
     * Offline permission check. Constructs a temporary
     * {@link PermissibleBase} to evaluate the node without an active
     * session. OP always passes.
     */
    public static boolean has(OfflinePlayer player, String node) {
        if (player != null) {
            if (player.isOp()) {
                return true;
            }
            PermissibleBase pb = new PermissibleBase(player);
            return pb.hasPermission(node);
        }
        return false;
    }

    /** Extended-mode offline check. See {@link #hasExtended(Player, String)}. */
    public static boolean hasExtended(OfflinePlayer player, String node) {
        return !Configuration.Permissions.EXTENDED || has(player, node);
    }

    /** Extended-mode offline check with explicit default. */
    public static boolean hasExtended(OfflinePlayer player, String node, boolean defaultValue) {
        if (Configuration.Permissions.EXTENDED) {
            return has(player, node);
        }
        return defaultValue;
    }
}

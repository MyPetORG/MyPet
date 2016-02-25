/*
 * This file is part of mypet-api
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet-api is licensed under the GNU Lesser General Public License.
 *
 * mypet-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet-api is distributed in the hope that it will be useful,
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

public class Permissions {

    public static boolean has(MyPetPlayer player, String node) {
        if (player != null && player.isOnline()) {
            return has(player.getPlayer(), node);
        }
        return false;
    }

    public static boolean has(Player player, String node) {
        if (player != null) {
            return !Configuration.Permissions.ENABLED || player.hasPermission(node);
        }
        return false;
    }

    public static boolean has(Player player, String node, boolean defaultValue) {
        if (player != null) {
            if (Configuration.Permissions.ENABLED) {
                return player.isOp() || player.hasPermission(node);
            }
            return defaultValue || player.isOp();
        }
        return false;
    }

    public static boolean hasExtended(Player player, String node) {
        return !Configuration.Permissions.EXTENDED || has(player, node);
    }

    public static boolean hasExtended(Player player, String node, boolean defaultValue) {
        if (Configuration.Permissions.EXTENDED) {
            return has(player, node, defaultValue);
        }
        return defaultValue;
    }

    public static boolean has(OfflinePlayer player, String node) {
        if (player != null) {
            if (!Configuration.Permissions.ENABLED || player.isOp()) {
                return true;
            } else {
                PermissibleBase pb = new PermissibleBase(player);
                return pb.hasPermission(node);
            }
        }
        return false;
    }

    public static boolean has(OfflinePlayer player, String node, boolean defaultValue) {
        if (player != null) {
            if (player.isOp()) {
                return true;
            } else if (Configuration.Permissions.ENABLED) {
                PermissibleBase pb = new PermissibleBase(player);
                return pb.hasPermission(node);
            } else {
                return defaultValue;
            }
        }
        return false;
    }

    public static boolean hasExtended(OfflinePlayer player, String node) {
        return !Configuration.Permissions.EXTENDED || has(player, node);
    }

    public static boolean hasExtended(OfflinePlayer player, String node, boolean defaultValue) {
        if (Configuration.Permissions.EXTENDED) {
            return has(player, node, defaultValue);
        }
        return defaultValue;
    }
}
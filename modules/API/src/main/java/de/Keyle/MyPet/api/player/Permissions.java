/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import de.Keyle.MyPet.api.Configuration;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;

public class Permissions {

    private static Multimap<String, String> legacy = ArrayListMultimap.create();

    static {
        legacy.put("MyPet.command.info.other", "MyPet.user.command.info.other");
        legacy.put("MyPet.command.capturehelper", "MyPet.user.command.capturehelper");
        legacy.put("MyPet.command.release", "MyPet.user.command.release");
        legacy.put("MyPet.command.respawn", "MyPet.user.command.respawn");
        legacy.put("MyPet.command.name", "MyPet.user.command.name");
        legacy.put("MyPet.command.name.color", "MyPet.user.command.name.color");
        legacy.put("MyPet.command.switch", "MyPet.user.command.switch");
        legacy.put("MyPet.command.switch.bypass", "MyPet.user.command.switch.bypass");
        legacy.put("MyPet.command.switch.limit.", "MyPet.user.command.switch.limit.");
        legacy.put("MyPet.command.trade.offer", "MyPet.user.command.trade.offer");
        legacy.put("MyPet.command.trade.offer.type.", "MyPet.user.command.offer.type.");
        legacy.put("MyPet.command.trade.receive", "MyPet.user.command.trade.recieve");
        legacy.put("MyPet.command.trade.receive", "MyPet.command.trade.recieve");
        legacy.put("MyPet.command.trade.receive.type.", "MyPet.user.trade.recieve.type.");
        legacy.put("MyPet.command.trade.receive.type.", "MyPet.trade.recieve.type.");
        legacy.put("MyPet.leash.", "MyPet.user.leash.");
        legacy.put("MyPet.skilltree.", "MyPet.custom.skilltree.");
        legacy.put("MyPet.extended.feed", "MyPet.user.extended.CanFeed");
        legacy.put("MyPet.extended.beacon", "MyPet.user.extended.Beacon");
        legacy.put("MyPet.extended.behavior.", "MyPet.user.extended.Behavior.");
        legacy.put("MyPet.extended.inventory", "MyPet.user.extended.Inventory");
        legacy.put("MyPet.extended.ride", "MyPet.user.extended.Ride");
        legacy.put("MyPet.extended.control", "MyPet.user.extended.Control");
        legacy.put("MyPet.extended.pickup", "MyPet.user.extended.Pickup");
        legacy.put("MyPet.extended.equip", "MyPet.user.extended.Equip");
    }

    public static boolean hasLegacy(MyPetPlayer player, String node, Object parameter) {
        if (player != null && player.isOnline()) {
            return hasLegacy(player.getPlayer(), node, parameter);
        }
        return false;
    }

    public static boolean hasLegacy(MyPetPlayer player, String node) {
        if (player != null && player.isOnline()) {
            return hasLegacy(player.getPlayer(), node);
        }
        return false;
    }

    public static boolean has(MyPetPlayer player, String node) {
        if (player != null && player.isOnline()) {
            return has(player.getPlayer(), node);
        }
        return false;
    }

    public static boolean hasLegacy(Player player, String node, Object parameter) {
        if (player != null) {
            if (!Configuration.Permissions.ENABLED || player.isOp()) {
                return true;
            }
            if (Configuration.Permissions.LEGACY && legacy.containsKey(node)) {
                for (String permission : legacy.get(node)) {
                    if (player.hasPermission(permission + parameter)) {
                        return true;
                    }
                }
            }
            return player.hasPermission(node + parameter);
        }
        return false;
    }

    public static boolean hasLegacy(Player player, String node, boolean defaultValue) {
        if (player != null) {
            if (Configuration.Permissions.ENABLED) {
                if (player.isOp()) {
                    return true;
                }
                if (Configuration.Permissions.LEGACY && legacy.containsKey(node)) {
                    for (String permission : legacy.get(node)) {
                        if (player.hasPermission(permission)) {
                            return true;
                        }
                    }
                }
                player.hasPermission(node);
            }
            return defaultValue || player.isOp();
        }
        return false;
    }

    public static boolean hasLegacy(Player player, String node, Object parameter, boolean defaultValue) {
        if (player != null) {
            if (Configuration.Permissions.ENABLED) {
                if (player.isOp()) {
                    return true;
                }
                if (Configuration.Permissions.LEGACY && legacy.containsKey(node)) {
                    for (String permission : legacy.get(node)) {
                        if (player.hasPermission(permission + parameter)) {
                            return true;
                        }
                    }
                }
                return player.hasPermission(node + parameter);
            }
            return defaultValue || player.isOp();
        }
        return false;
    }

    public static boolean hasLegacy(Player player, String node) {
        if (player != null) {
            if (!Configuration.Permissions.ENABLED || player.isOp()) {
                return true;
            }
            if (Configuration.Permissions.LEGACY && legacy.containsKey(node)) {
                for (String permission : legacy.get(node)) {
                    if (player.hasPermission(permission)) {
                        return true;
                    }
                }
            }
            return player.hasPermission(node);
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

    public static boolean hasExtendedLegacy(Player player, String node) {
        return !Configuration.Permissions.EXTENDED || hasLegacy(player, node);
    }

    public static boolean hasExtendedLegacy(Player player, String node, Object parameter) {
        return !Configuration.Permissions.EXTENDED || hasLegacy(player, node, parameter);
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
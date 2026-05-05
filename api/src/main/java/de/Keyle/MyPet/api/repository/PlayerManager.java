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

package de.Keyle.MyPet.api.repository;

import de.Keyle.MyPet.api.player.MyPetPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the online {@link MyPetPlayer} cache. Tracks which players are
 * known to the MyPet system and provides lookup by UUID, Bukkit
 * {@link Player}, or name. The concrete implementation in the plugin
 * module handles persistence (load/save to the repository) and login/
 * logout lifecycle.
 * <p>
 * The cache is thread-safe ({@link ConcurrentHashMap}), but most
 * mutations should occur on the main/entity thread.
 */
public abstract class PlayerManager {
    protected final Map<UUID, MyPetPlayer> onlinePlayers = new ConcurrentHashMap<>();

    /**
     * Returns the cached MyPetPlayer for the given UUID, or {@code null}
     * if the player is not online or not registered.
     */
    public MyPetPlayer getMyPetPlayer(UUID playerUUID) {
        if (playerUUID != null) {
            return onlinePlayers.get(playerUUID);
        }
        return null;
    }

    /**
     * Returns the cached MyPetPlayer for the given Bukkit player, or
     * {@code null} if not registered.
     */
    public MyPetPlayer getMyPetPlayer(Player player) {
        if (player == null) {
            return null;
        }
        return getMyPetPlayer(player.getUniqueId());
    }

    /**
     * Looks up a MyPetPlayer by display name. Returns {@code null} if
     * the player is not online or not registered.
     */
    public MyPetPlayer getMyPetPlayer(String name) {
        Player player = Bukkit.getPlayer(name);
        return getMyPetPlayer(player);
    }

    /** Adds a player to the online cache. Called on join after loading. */
    public void setOnline(MyPetPlayer player) {
        onlinePlayers.put(player.getUniqueId(), player);
    }

    /** Removes a player from the online cache and persists their data. */
    public abstract void setOffline(MyPetPlayer player);

    /**
     * Creates a new MyPetPlayer instance for a Bukkit player without
     * persisting it. Used during the first-time registration flow.
     */
    public abstract MyPetPlayer createMyPetPlayer(Player player);

    /** Returns {@code true} if an online player with this name is registered. */
    public boolean isMyPetPlayer(String name) {
        Player player = Bukkit.getPlayer(name);
        return player != null && isMyPetPlayer(player);
    }

    /** Returns {@code true} if this Bukkit player is in the online cache. */
    public boolean isMyPetPlayer(Player player) {
        return onlinePlayers.containsKey(player.getUniqueId());
    }

    /** Returns a snapshot array of all currently online MyPetPlayers. */
    public MyPetPlayer[] getMyPetPlayers() {
        return onlinePlayers.values().toArray(new MyPetPlayer[0]);
    }

    /**
     * Loads or creates a MyPetPlayer for the given Bukkit player and adds
     * them to the online cache. Called during the login sequence.
     */
    public abstract MyPetPlayer registerMyPetPlayer(Player player);
}
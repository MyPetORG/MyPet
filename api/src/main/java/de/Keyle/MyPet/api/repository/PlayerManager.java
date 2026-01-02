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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class PlayerManager {
    protected final Map<UUID, MyPetPlayer> onlinePlayers = new ConcurrentHashMap<>();

    public MyPetPlayer getMyPetPlayer(UUID playerUUID) {
        if (playerUUID != null) {
            return onlinePlayers.get(playerUUID);
        }
        return null;
    }

    public MyPetPlayer getMyPetPlayer(Player player) {
        if (player == null) {
            return null;
        }
        return getMyPetPlayer(player.getUniqueId());
    }

    public MyPetPlayer getMyPetPlayer(String name) {
        Player player = Bukkit.getPlayer(name);
        return getMyPetPlayer(player);
    }

    public void setOnline(MyPetPlayer player) {
        onlinePlayers.put(player.getUniqueId(), player);
    }

    public void setOffline(MyPetPlayer player) {
        onlinePlayers.remove(player.getUniqueId());
        MyPetApi.getRepository().updateMyPetPlayer(player, null);
    }

    public abstract MyPetPlayer createMyPetPlayer(Player player);

    public boolean isMyPetPlayer(String name) {
        Player player = Bukkit.getPlayer(name);
        return player != null && isMyPetPlayer(player);
    }

    public boolean isMyPetPlayer(Player player) {
        return onlinePlayers.containsKey(player.getUniqueId());
    }

    public MyPetPlayer[] getMyPetPlayers() {
        return onlinePlayers.values().toArray(new MyPetPlayer[0]);
    }

    public MyPetPlayer registerMyPetPlayer(Player player) {
        MyPetPlayer myPetPlayer = createMyPetPlayer(player);
        MyPetApi.getRepository().addMyPetPlayer(myPetPlayer, null);
        setOnline(myPetPlayer);
        return myPetPlayer;
    }
}
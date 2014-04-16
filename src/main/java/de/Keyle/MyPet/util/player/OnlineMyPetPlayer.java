/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

package de.Keyle.MyPet.util.player;

import de.Keyle.MyPet.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class OnlineMyPetPlayer extends MyPetPlayer {
    protected OnlineMyPetPlayer(UUID playerUUID, UUID mojangUUID) {
        super(playerUUID);
        this.mojangUUID = mojangUUID;
        uuidToInternalUUID.put(mojangUUID, internalUUID);
    }

    protected OnlineMyPetPlayer(UUID mojangUUID) {
        this(UUID.randomUUID(), mojangUUID);
    }

    public boolean isOnline() {
        return onlinePlayerUUIDList.contains(mojangUUID);
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(mojangUUID);
    }

    public void setLastKnownName(String name) {
        this.lastKnownPlayerName = name;
    }

    @Override
    public String getName() {
        if (this.lastKnownPlayerName == null) {
            if (isOnline()) {
                this.lastKnownPlayerName = getPlayer().getName();
            } else {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(mojangUUID);
                if (!offlinePlayer.getName().equals("InvalidUUID")) {
                    this.lastKnownPlayerName = offlinePlayer.getName();
                    offlineUUID = Util.getOfflinePlayerUUID(lastKnownPlayerName);
                }
            }
        }
        return lastKnownPlayerName;
    }

    @Override
    public String toString() {
        return "Online" + super.toString();
    }
}
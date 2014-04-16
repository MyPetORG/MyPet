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
import org.bukkit.entity.Player;

import java.util.UUID;

public class OfflineMyPetPlayer extends MyPetPlayer {
    protected OfflineMyPetPlayer(UUID internalUUID, String playerName) {
        super(internalUUID);
        this.lastKnownPlayerName = playerName;
        offlineUUID = Util.getOfflinePlayerUUID(getName());
        uuidToInternalUUID.put(offlineUUID, internalUUID);
    }

    protected OfflineMyPetPlayer(String playerName) {
        this(UUID.randomUUID(), playerName);
    }

    public boolean isOnline() {
        return onlinePlayerUUIDList.contains(offlineUUID);
    }

    public Player getPlayer() {
        return Bukkit.getServer().getPlayerExact(getName());
    }

    @Override
    public String toString() {
        return "Offline" + super.toString();
    }
}
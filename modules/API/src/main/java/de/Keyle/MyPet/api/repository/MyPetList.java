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

package de.Keyle.MyPet.api.repository;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.ActiveMyPet;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.event.MyPetSelectEvent;
import de.Keyle.MyPet.api.event.MyPetSelectEvent.NewStatus;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import org.bukkit.entity.Player;

import static org.bukkit.Bukkit.getServer;

public abstract class MyPetList {
    protected final BiMap<MyPetPlayer, ActiveMyPet> mActivePlayerPets = HashBiMap.create();
    protected final BiMap<ActiveMyPet, MyPetPlayer> mActivePetsPlayer = mActivePlayerPets.inverse();

    // Active -------------------------------------------------------------------

    public ActiveMyPet getMyPet(MyPetPlayer owner) {
        return mActivePlayerPets.get(owner);
    }

    public ActiveMyPet getMyPet(Player owner) {
        return mActivePlayerPets.get(MyPetApi.getPlayerList().getMyPetPlayer(owner));
    }

    public ActiveMyPet[] getAllActiveMyPets() {
        ActiveMyPet[] allActiveMyPets = new ActiveMyPet[mActivePetsPlayer.keySet().size()];
        int i = 0;
        for (ActiveMyPet myPet : mActivePetsPlayer.keySet()) {
            allActiveMyPets[i++] = myPet;
        }
        return allActiveMyPets;
    }

    public boolean hasActiveMyPet(MyPetPlayer player) {
        return mActivePlayerPets.containsKey(player);
    }

    public boolean hasActiveMyPet(Player player) {
        if (MyPetApi.getPlayerList().isMyPetPlayer(player)) {
            MyPetPlayer petPlayer = MyPetApi.getPlayerList().getMyPetPlayer(player);
            return hasActiveMyPet(petPlayer);
        }
        return false;
    }

    public boolean hasActiveMyPet(String name) {
        if (MyPetApi.getPlayerList().isMyPetPlayer(name)) {
            MyPetPlayer petPlayer = MyPetApi.getPlayerList().getMyPetPlayer(name);
            return hasActiveMyPet(petPlayer);
        }
        return false;
    }

    // Inactive -----------------------------------------------------------------

    public abstract MyPet getInactiveMyPetFromMyPet(MyPet activeMyPet);

    // All ----------------------------------------------------------------------

    public abstract ActiveMyPet activateMyPet(MyPet inactiveMyPet);

    public boolean deactivateMyPet(MyPetPlayer owner, boolean update) {
        if (mActivePlayerPets.containsKey(owner)) {
            final ActiveMyPet activeMyPet = owner.getMyPet();

            MyPetSelectEvent event = new MyPetSelectEvent(activeMyPet, NewStatus.Inactive);
            getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            }

            activeMyPet.removePet();
            if (update) {
                MyPetApi.getRepository().updateMyPet(activeMyPet, null);
            }
            mActivePetsPlayer.remove(activeMyPet);
            return true;
        }
        return false;
    }

    public int countActiveMyPets() {
        return mActivePetsPlayer.size();
    }
}
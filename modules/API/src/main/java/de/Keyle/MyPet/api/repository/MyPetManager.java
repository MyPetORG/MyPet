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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.event.MyPetSaveEvent;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;

public abstract class MyPetManager {
    protected final BiMap<MyPetPlayer, MyPet> mActivePlayerPets = HashBiMap.create();
    protected final BiMap<MyPet, MyPetPlayer> mActivePetsPlayer = mActivePlayerPets.inverse();

    // Active -------------------------------------------------------------------

    public MyPet getMyPet(MyPetPlayer owner) {
        return mActivePlayerPets.get(owner);
    }

    public MyPet getMyPet(Player owner) {
        return mActivePlayerPets.get(MyPetApi.getPlayerManager().getMyPetPlayer(owner));
    }

    public MyPet[] getAllActiveMyPets() {
        MyPet[] allActiveMyPets = new MyPet[mActivePetsPlayer.keySet().size()];
        int i = 0;
        for (MyPet myPet : mActivePetsPlayer.keySet()) {
            allActiveMyPets[i++] = myPet;
        }
        return allActiveMyPets;
    }

    public boolean hasActiveMyPet(MyPetPlayer player) {
        return mActivePlayerPets.containsKey(player);
    }

    public boolean hasActiveMyPet(Player player) {
        if (MyPetApi.getPlayerManager().isMyPetPlayer(player)) {
            MyPetPlayer petPlayer = MyPetApi.getPlayerManager().getMyPetPlayer(player);
            return hasActiveMyPet(petPlayer);
        }
        return false;
    }

    public boolean hasActiveMyPet(String name) {
        if (MyPetApi.getPlayerManager().isMyPetPlayer(name)) {
            MyPetPlayer petPlayer = MyPetApi.getPlayerManager().getMyPetPlayer(name);
            return hasActiveMyPet(petPlayer);
        }
        return false;
    }

    // Inactive -----------------------------------------------------------------

    public abstract StoredMyPet getInactiveMyPetFromMyPet(StoredMyPet storedMyPet);

    // All ----------------------------------------------------------------------

    public abstract Optional<MyPet> activateMyPet(StoredMyPet storedMyPet);

    public boolean deactivateMyPet(MyPetPlayer owner, boolean update) {
        if (mActivePlayerPets.containsKey(owner)) {
            final MyPet myPet = owner.getMyPet();

            MyPetSaveEvent event = new MyPetSaveEvent(myPet);
            Bukkit.getServer().getPluginManager().callEvent(event);

            myPet.removePet();
            if (update) {
                MyPetApi.getRepository().updateMyPet(myPet, null);
            }
            mActivePetsPlayer.remove(myPet);
            return true;
        }
        return false;
    }

    public int countActiveMyPets() {
        return mActivePetsPlayer.size();
    }
}
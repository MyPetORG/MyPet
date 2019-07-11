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

import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public interface Repository {
    void disable();

    void save();

    void init() throws RepositoryInitException;

    List<StoredMyPet> getAllMyPets();

    List<MyPetPlayer> getAllMyPetPlayers();

    void cleanup(long timestamp, final RepositoryCallback<Integer> callback);

    void countMyPets(final RepositoryCallback<Integer> callback);

    void countMyPets(final MyPetType type, final RepositoryCallback<Integer> callback);

    void hasMyPets(final MyPetPlayer myPetPlayer, final RepositoryCallback<Boolean> callback);

    void getMyPets(final MyPetPlayer owner, final RepositoryCallback<List<StoredMyPet>> callback);

    void getMyPet(final UUID uuid, final RepositoryCallback<StoredMyPet> callback);

    void removeMyPet(final UUID uuid, final RepositoryCallback<Boolean> callback);

    void removeMyPet(final StoredMyPet storedMyPet, final RepositoryCallback<Boolean> callback);

    void addMyPet(final StoredMyPet storedMyPet, final RepositoryCallback<Boolean> callback);

    void updateMyPet(final StoredMyPet storedMyPet, final RepositoryCallback<Boolean> callback);

    void isMyPetPlayer(final Player player, final RepositoryCallback<Boolean> callback);

    void getMyPetPlayer(final UUID uuid, final RepositoryCallback<MyPetPlayer> callback);

    void getMyPetPlayer(final Player player, final RepositoryCallback<MyPetPlayer> callback);

    void updateMyPetPlayer(final MyPetPlayer player, final RepositoryCallback<Boolean> callback);

    void addMyPetPlayer(final MyPetPlayer player, final RepositoryCallback<Boolean> callback);

    void removeMyPetPlayer(final MyPetPlayer player, final RepositoryCallback<Boolean> callback);
}
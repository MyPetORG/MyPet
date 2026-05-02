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

package de.Keyle.MyPet.repository;

import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface Repository {
    void disable();

    void save();

    void init() throws RepositoryInitException;

    CompletableFuture<List<StoredMyPet>> getAllPets();

    CompletableFuture<List<MyPetPlayer>> getAllMyPetPlayers();

    CompletableFuture<Integer> cleanup(long timestamp);

    CompletableFuture<Integer> countPets();

    CompletableFuture<Integer> countPets(MyPetType type);

    CompletableFuture<Boolean> hasPets(MyPetPlayer myPetPlayer);

    CompletableFuture<List<StoredMyPet>> getPets(MyPetPlayer owner);

    CompletableFuture<StoredMyPet> getPet(UUID uuid);

    CompletableFuture<Boolean> removePet(UUID uuid);

    CompletableFuture<Boolean> removePet(StoredMyPet storedMyPet);

    CompletableFuture<Boolean> addPet(StoredMyPet storedMyPet);

    CompletableFuture<Boolean> savePet(StoredMyPet storedMyPet);

    CompletableFuture<Boolean> updatePet(StoredMyPet storedMyPet);

    CompletableFuture<Boolean> isMyPetPlayer(Player player);

    CompletableFuture<MyPetPlayer> getMyPetPlayer(UUID uuid);

    CompletableFuture<MyPetPlayer> getMyPetPlayer(Player player);

    CompletableFuture<Boolean> updateMyPetPlayer(MyPetPlayer player);

    CompletableFuture<Boolean> addMyPetPlayer(MyPetPlayer player);

    CompletableFuture<Boolean> removeMyPetPlayer(MyPetPlayer player);
}

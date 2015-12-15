/*
 * This file is part of MyPet-1.8
 *
 * Copyright (C) 2011-2015 Keyle
 * MyPet-1.8 is licensed under the GNU Lesser General Public License.
 *
 * MyPet-1.8 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet-1.8 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.repository;

import de.Keyle.MyPet.entity.types.InactiveMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.entity.types.MyPetType;
import de.Keyle.MyPet.util.player.MyPetPlayer;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface Repository {
    void disable();

    void save();

    void init();

    int countMyPets();

    int countMyPets(MyPetType type);

    Collection<InactiveMyPet> getAllMyPets();

    boolean hasMyPets(MyPetPlayer myPetPlayer);

    List<InactiveMyPet> getMyPets(MyPetPlayer owner);

    InactiveMyPet getMyPet(UUID uuid);

    void removeMyPet(UUID inactiveMyPet);

    void removeMyPet(InactiveMyPet inactiveMyPet);

    void addMyPet(InactiveMyPet inactiveMyPet);

    boolean updateMyPet(MyPet myPet);

    boolean isMyPetPlayer(Player player);

    MyPetPlayer getMyPetPlayer(UUID uuid);

    MyPetPlayer getMyPetPlayer(Player player);

    void updatePlayer(MyPetPlayer player);

    void addMyPetPlayer(MyPetPlayer player);

    void removeMyPetPlayer(MyPetPlayer player);
}
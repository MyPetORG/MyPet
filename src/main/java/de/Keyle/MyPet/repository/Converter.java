/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2015 Keyle
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

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.repository.Repository;
import de.Keyle.MyPet.entity.types.InactiveMyPet;
import de.Keyle.MyPet.repository.types.MySqlRepository;
import de.Keyle.MyPet.repository.types.NbtRepository;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import de.Keyle.MyPet.util.player.MyPetPlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Converter {
    public static String CONVERT_FROM = "-";

    public static boolean convert() {
        if (CONVERT_FROM.equalsIgnoreCase(MyPetPlugin.REPOSITORY_TYPE)) {
            return false;
        }

        Repository fromRepo;
        Repository toRepo;

        if (CONVERT_FROM.equalsIgnoreCase("NBT")) {
            fromRepo = new NbtRepository();
        } else if (CONVERT_FROM.equalsIgnoreCase("MySQL")) {
            fromRepo = new MySqlRepository();
        } else {
            return false;
        }

        try {
            fromRepo.init();
        } catch (RepositoryInitException e) {
            return false;
        }

        toRepo = MyPetPlugin.getPlugin().getRepository();

        List<MyPetPlayer> playerList = fromRepo.getAllMyPetPlayers();
        Map<UUID, MyPetPlayer> players = new HashMap<>();

        for (MyPetPlayer player : playerList) {
            players.put(player.getInternalUUID(), player);
        }

        List<InactiveMyPet> pets = fromRepo.getAllMyPets(players);

        for (MyPetPlayer player : playerList) {
            if (toRepo instanceof NbtRepository) {
                toRepo.addMyPetPlayer(player, null);
            } else if (toRepo instanceof MySqlRepository) {
                ((MySqlRepository) toRepo).addMyPetPlayer(player);
            }
        }

        for (InactiveMyPet pet : pets) {
            if (toRepo instanceof NbtRepository) {
                toRepo.addMyPet(pet, null);
            } else if (toRepo instanceof MySqlRepository) {
                ((MySqlRepository) toRepo).addMyPet(pet);
            }
        }

        toRepo.save();
        fromRepo.disable();

        MyPetPlugin.getPlugin().getConfig().set("MyPet.Repository.ConvertFrom", "-");
        MyPetPlugin.getPlugin().saveConfig();

        MyPetLogger.write("Conversion from " + CONVERT_FROM + " to " + MyPetPlugin.REPOSITORY_TYPE + " complete!");

        return true;
    }
}
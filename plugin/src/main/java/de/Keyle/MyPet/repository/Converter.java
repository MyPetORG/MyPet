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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.repository.Repository;
import de.Keyle.MyPet.api.repository.RepositoryInitException;
import de.Keyle.MyPet.repository.types.MongoDbRepository;
import de.Keyle.MyPet.repository.types.MySqlRepository;
import de.Keyle.MyPet.repository.types.NbtRepository;
import de.Keyle.MyPet.repository.types.SqLiteRepository;

import java.io.File;
import java.util.HashSet;
import java.util.List;

public class Converter {

    public static boolean convert() {
        if (Configuration.Repository.CONVERT_FROM.equalsIgnoreCase(Configuration.Repository.REPOSITORY_TYPE)) {
            return false;
        }

        Repository fromRepo;
        Repository toRepo;

        if (Configuration.Repository.CONVERT_FROM.equalsIgnoreCase("NBT")) {
            fromRepo = new NbtRepository();
        } else if (Configuration.Repository.CONVERT_FROM.equalsIgnoreCase("MySQL")) {
            fromRepo = new MySqlRepository();
        } else if (Configuration.Repository.CONVERT_FROM.equalsIgnoreCase("MongoDB")) {
            fromRepo = new MongoDbRepository();
        } else if (Configuration.Repository.CONVERT_FROM.equalsIgnoreCase("SQLite")) {
            fromRepo = new SqLiteRepository();
        } else {
            return false;
        }

        MyPetApi.getLogger().info("Converting from " + Configuration.Repository.CONVERT_FROM + " to " + Configuration.Repository.REPOSITORY_TYPE + "...");

        try {
            fromRepo.init();
        } catch (RepositoryInitException e) {
            return false;
        }

        toRepo = MyPetApi.getRepository();

        List<MyPetPlayer> playerList = fromRepo.getAllMyPetPlayers();
        if (toRepo instanceof NbtRepository) {
            return false;
        } else if (toRepo instanceof MySqlRepository) {
            ((MySqlRepository) toRepo).addMyPetPlayers(playerList);
        } else if (toRepo instanceof SqLiteRepository) {
            ((SqLiteRepository) toRepo).addMyPetPlayers(playerList);
        } else if (toRepo instanceof MongoDbRepository) {
            HashSet<String> playerNames = new HashSet<>();
            for (MyPetPlayer player : playerList) {
                String playerName = player.getName();
                if (playerNames.contains(playerName)) {
                    MyPetApi.getLogger().info("Found duplicate Player: " + player.toString());
                    continue;
                }
                playerNames.add(playerName);
                ((MongoDbRepository) toRepo).addMyPetPlayer(player);
            }
        }

        List<StoredMyPet> pets = fromRepo.getAllMyPets();

        if (toRepo instanceof MySqlRepository) {
            ((MySqlRepository) toRepo).addMyPets(pets);
        } else if (toRepo instanceof SqLiteRepository) {
            ((SqLiteRepository) toRepo).addMyPets(pets);
        } else if (toRepo instanceof MongoDbRepository) {
            for (StoredMyPet pet : pets) {
                ((MongoDbRepository) toRepo).addMyPet(pet);
            }
        }

        toRepo.save();
        fromRepo.disable();

        if (Configuration.Repository.CONVERT_FROM.equalsIgnoreCase("NBT")) {
            File nbtFile = new File(MyPetApi.getPlugin().getDataFolder().getPath() + File.separator + "My.Pets");
            File nbtFileOld = new File(MyPetApi.getPlugin().getDataFolder().getPath() + File.separator + "My.Pets.old");
            nbtFile.renameTo(nbtFileOld);
            MyPetApi.getPlugin().getConfig().set("MyPet.Repository.Type", Configuration.Repository.REPOSITORY_TYPE);
            MyPetApi.getPlugin().getConfig().set("MyPet.Repository.NBT", null);
        }
        MyPetApi.getPlugin().getConfig().set("MyPet.Repository.ConvertFrom", "-");
        MyPetApi.getPlugin().saveConfig();

        MyPetApi.getLogger().info("Conversion from " + Configuration.Repository.CONVERT_FROM + " to " + Configuration.Repository.REPOSITORY_TYPE + " complete!");

        return true;
    }
}
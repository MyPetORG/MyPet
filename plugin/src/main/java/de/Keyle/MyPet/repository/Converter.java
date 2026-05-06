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
import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.entity.StoredPet;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.repository.types.AbstractSqlRepository;
import de.Keyle.MyPet.repository.types.MySqlRepository;
import de.Keyle.MyPet.repository.types.SqLiteRepository;

import java.util.List;

public class Converter {

    public static boolean convert() {
        if (Configuration.Repository.CONVERT_FROM.equalsIgnoreCase(Configuration.Repository.REPOSITORY_TYPE)) {
            return false;
        }

        Repository fromRepo;
        Repository toRepo;

        if (Configuration.Repository.CONVERT_FROM.equalsIgnoreCase("MySQL")) {
            fromRepo = new MySqlRepository();
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

        toRepo = MyPetPlugin.getInstance().getRepository();

        List<MyPetPlayer> playerList = fromRepo.getAllMyPetPlayers().join();
        if (toRepo instanceof AbstractSqlRepository sql) {
            sql.addMyPetPlayers(playerList);
        }

        List<StoredPet> pets = fromRepo.getAllPets().join();
        if (toRepo instanceof AbstractSqlRepository sql) {
            sql.addPets(pets);
        }

        toRepo.save();
        fromRepo.disable();

        MyPetApi.getPlugin().getConfig().set("MyPet.Repository.ConvertFrom", "-");
        MyPetApi.getPlugin().saveConfig();

        MyPetApi.getLogger().info("Conversion from " + Configuration.Repository.CONVERT_FROM + " to " + Configuration.Repository.REPOSITORY_TYPE + " complete!");

        return true;
    }
}

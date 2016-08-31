/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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
import de.Keyle.MyPet.repository.types.NbtRepository;
import de.Keyle.MyPet.repository.types.SqLiteRepository;

import java.io.File;
import java.util.List;

public class Converter {

    public static boolean convert() {
        MyPetApi.getLogger().info("Converting from NBT to " + Configuration.Repository.REPOSITORY_TYPE + "...");

        NbtRepository fromRepo = new NbtRepository();
        fromRepo.init();

        SqLiteRepository toRepo = (SqLiteRepository) MyPetApi.getRepository();

        List<MyPetPlayer> playerList = fromRepo.getAllMyPetPlayers();
        toRepo.addMyPetPlayers(playerList);

        List<StoredMyPet> pets = fromRepo.getAllMyPets();
        toRepo.addMyPets(pets);


        toRepo.save();
        fromRepo.disable();

        File nbtFile = new File(MyPetApi.getPlugin().getDataFolder().getPath() + File.separator + "My.Pets");
        File nbtFileOld = new File(MyPetApi.getPlugin().getDataFolder().getPath() + File.separator + "My.Pets.old");
        nbtFile.renameTo(nbtFileOld);
        MyPetApi.getPlugin().getConfig().set("MyPet.Repository.NBT", null);
        MyPetApi.getPlugin().saveConfig();

        MyPetApi.getLogger().info("Conversion from NBT to " + Configuration.Repository.REPOSITORY_TYPE + " complete!");

        return true;
    }
}
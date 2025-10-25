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

package de.Keyle.MyPet.services;

import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.util.service.Load;
import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagInt;

@Load(Load.State.AfterHooks)
public class RepositoryMyPetConverterService extends de.Keyle.MyPet.api.util.service.types.RepositoryMyPetConverterService {

    public void v1_11_R1(StoredMyPet pet) {
        TagCompound info = pet.getInfo();

        switch (pet.getPetType()) {
            case Horse:
                if (info.containsKey("Type")) {
                    byte type = info.getAs("Type", TagByte.class).getByteData();
                    switch (type) {
                        case 1:
                            pet.setPetType(MyPetType.Donkey);
                            break;
                        case 2:
                            pet.setPetType(MyPetType.Mule);
                            break;
                        case 3:
                            pet.setPetType(MyPetType.ZombieHorse);
                            break;
                        case 4:
                            pet.setPetType(MyPetType.SkeletonHorse);
                            break;
                    }
                    info.remove("Type");
                }
                break;
            case Skeleton:
                if (info.containsKey("Type")) {
                    int type = info.getAs("Type", TagInt.class).getIntData();
                    switch (type) {
                        case 1:
                            pet.setPetType(MyPetType.WitherSkeleton);
                            break;
                        case 2:
                            pet.setPetType(MyPetType.Stray);
                            break;
                    }
                    info.remove("Type");
                }
                if (info.containsKey("Wither")) {
                    if (info.getAs("Wither", TagByte.class).getBooleanData()) {
                        pet.setPetType(MyPetType.WitherSkeleton);
                        info.remove("Wither");
                    }
                }
                break;
            case Zombie:
                if (info.containsKey("Type")) {
                    int type = info.getAs("Type", TagInt.class).getIntData();
                    switch (type) {
                        case 6:
                            pet.setPetType(MyPetType.Husk);
                            break;
                        case 0:
                            break;
                        default:
                            if (type == 2) {
                                if (info.containsKey("Profession")) {
                                    pet.setPetType(MyPetType.ZombieVillager);
                                    info.put("Profession", new TagInt(info.getAs("Profession", TagInt.class).getIntData() - 1));
                                } else {
                                    pet.setPetType(MyPetType.Husk);
                                }
                            } else {
                                pet.setPetType(MyPetType.ZombieVillager);
                                if (info.containsKey("Profession")) {
                                    info.put("Profession", new TagInt(info.getAs("Profession", TagInt.class).getIntData() - 1));
                                }
                            }
                    }
                    info.remove("Type");
                }
                if (info.containsKey("Villager")) {
                    if (info.getAs("Villager", TagByte.class).getBooleanData()) {
                        pet.setPetType(MyPetType.ZombieVillager);
                        info.remove("Villager");
                    }
                }
                if (info.containsKey("Husk")) {
                    if (info.getAs("Husk", TagByte.class).getBooleanData()) {
                        pet.setPetType(MyPetType.Husk);
                        info.remove("Husk");
                    }
                }
                break;
            case Guardian:
                if (info.containsKey("Elder")) {
                    if (info.getAs("Elder", TagByte.class).getBooleanData()) {
                        pet.setPetType(MyPetType.ElderGuardian);
                        info.remove("Elder");
                    }
                }
                break;
        }
    }
}

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

package de.Keyle.MyPet.compat.v1_14_R1.services;

import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.entity.types.MyVillager;
import de.Keyle.MyPet.api.util.service.Load;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagInt;
import org.bukkit.entity.Cat;

@Load(Load.State.AfterHooks)
public class RepositoryMyPetConverterService extends de.Keyle.MyPet.api.util.service.types.RepositoryMyPetConverterService {

    public void v1_14_R1(StoredMyPet pet) {
        TagCompound info = pet.getInfo();

        switch (pet.getPetType()) {
            case Villager:
                if (info.containsKey("VillagerLevel")) {
                    return;
                }
            case ZombieVillager:
                if (info.containsKey("TradingLevel")) {
                    return;
                }
                if (info.containsKeyAs("Profession", TagInt.class)) {
                    int professionId = info.getAs("Profession", TagInt.class).getIntData();
                    int career = 1;
                    if (info.containsKey("OriginalData")) {
                        TagCompound originalData = info.get("OriginalData");
                        if (originalData.containsKey("Career")) {
                            career = originalData.getAs("Career", TagInt.class).getIntData();
                        }
                    }
                    switch (professionId) {
                        case 0:
                            switch (career) {
                                case 1:
                                    info.put("Profession", new TagInt(MyVillager.Profession.FARMER.ordinal()));
                                    break;
                                case 2:
                                    info.put("Profession", new TagInt(MyVillager.Profession.FISHERMAN.ordinal()));
                                    break;
                                case 3:
                                    info.put("Profession", new TagInt(MyVillager.Profession.SHEPHERD.ordinal()));
                                    break;
                                case 4:
                                    info.put("Profession", new TagInt(MyVillager.Profession.FLETCHER.ordinal()));
                                    break;
                            }
                            break;
                        case 1:
                            switch (career) {
                                case 1:
                                    info.put("Profession", new TagInt(MyVillager.Profession.LIBRARIAN.ordinal()));
                                    break;
                                case 2:
                                    info.put("Profession", new TagInt(MyVillager.Profession.CARTOGRAPHER.ordinal()));
                                    break;
                            }
                            break;
                        case 2:
                            info.put("Profession", new TagInt(MyVillager.Profession.CLERIC.ordinal()));
                            break;
                        case 3:
                            switch (career) {
                                case 1:
                                    info.put("Profession", new TagInt(MyVillager.Profession.ARMORER.ordinal()));
                                    break;
                                case 2:
                                    info.put("Profession", new TagInt(MyVillager.Profession.WEAPONSMITH.ordinal()));
                                    break;
                                case 3:
                                    info.put("Profession", new TagInt(MyVillager.Profession.TOOLSMITH.ordinal()));
                                    break;
                            }
                            break;
                        case 4:
                            switch (career) {
                                case 1:
                                    info.put("Profession", new TagInt(MyVillager.Profession.BUTCHER.ordinal()));
                                    break;
                                case 2:
                                    info.put("Profession", new TagInt(MyVillager.Profession.LEATHERWORKER.ordinal()));
                                    break;
                            }
                            break;
                        case 5:
                            info.put("Profession", new TagInt(MyVillager.Profession.NITWIT.ordinal()));
                            break;
                    }
                }
                break;
            case Ocelot:
                if (info.containsKey("CatType")) {
                    pet.setPetType(MyPetType.Cat);
                    int catType = info.getAs("CatType", TagInt.class).getIntData();
                    if (catType > 0) {
                        switch (catType) {
                            case 1:
                                info.put("CatType", new TagInt(Cat.Type.BLACK.ordinal()));
                                break;
                            case 2:
                                info.put("CatType", new TagInt(Cat.Type.RED.ordinal()));
                                break;
                            case 3:
                                info.put("CatType", new TagInt(Cat.Type.SIAMESE.ordinal()));
                                break;
                            default:
                                info.put("CatType", new TagInt(Cat.Type.TABBY.ordinal()));
                                break;
                        }
                    }
                }
                break;
        }

        pet.setInfo(info);
    }
}

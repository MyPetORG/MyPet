package de.Keyle.MyPet.services;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.util.service.Load;
import de.keyle.knbt.TagByte;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagInt;
import de.keyle.knbt.TagString;

@Load(Load.State.AfterHooks)
public class RepositoryMyPetConverterService implements de.Keyle.MyPet.api.util.service.types.RepositoryMyPetConverterService {

    Version toVersion;

    @Override
    public boolean onEnable() {
        try {
            toVersion = Version.valueOf(MyPetApi.getCompatUtil().getInternalVersion());
        } catch (Throwable e) {
            return false;
        }
        return true;
    }

    @Override
    public void convert(StoredMyPet pet) {
        Version fromVersion = Version.v1_7_R4;

        TagCompound info = pet.getInfo();
        if (info.containsKey("Version")) {
            if (info.containsKeyAs("Version", TagString.class)) {
                fromVersion = Version.valueOf(info.getAs("Version", TagString.class).getStringData());
            } else {
                fromVersion = Version.values()[info.getAs("Version", TagInt.class).getIntData()];
            }
        }
        if (fromVersion != toVersion) {
            switch (toVersion) {
                case v1_11_R1:
                case v1_12_R1:
                    v1_11_R1(pet);
                    break;
            }
        }
    }

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

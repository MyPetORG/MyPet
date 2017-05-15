package de.Keyle.MyPet.api.util.service.types;

import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.util.service.ServiceContainer;

public interface RepositoryMyPetConverterService extends ServiceContainer {
    enum Version {
        v1_7_R4,
        v1_8_R1,
        v1_8_R2,
        v1_8_R3,
        v1_9_R1,
        v1_9_R2,
        v1_10_R1,
        v1_11_R1,
        v1_12_R1,
        UNKNOWN
    }

    void convert(StoredMyPet pet);
}

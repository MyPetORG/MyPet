package de.Keyle.MyPet.compat.v1_9_R1;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_9_R1.services.EggIconService;
import de.Keyle.MyPet.compat.v1_9_R1.services.EntityConverterService;

@Compat("v1_9_R1")
public class CompatManager extends de.Keyle.MyPet.api.util.CompatManager {
    public void init() {
        MyPetApi.getServiceManager().registerService(EggIconService.class);
        MyPetApi.getServiceManager().registerService(EntityConverterService.class);
    }
}

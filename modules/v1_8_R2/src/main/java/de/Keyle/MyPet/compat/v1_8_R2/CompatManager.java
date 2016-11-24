package de.Keyle.MyPet.compat.v1_8_R2;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_8_R2.services.EggIconService;
import de.Keyle.MyPet.compat.v1_8_R2.services.EntityConverterService;

@Compat("v1_8_R2")
public class CompatManager extends de.Keyle.MyPet.api.util.CompatManager {
    public void init() {
        MyPetApi.getServiceManager().registerService(EggIconService.class);
        MyPetApi.getServiceManager().registerService(EntityConverterService.class);
    }
}

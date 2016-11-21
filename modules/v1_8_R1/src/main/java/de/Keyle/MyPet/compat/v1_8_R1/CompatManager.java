package de.Keyle.MyPet.compat.v1_8_R1;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_8_R1.services.EggIconService;

@Compat("v1_8_R1")
public class CompatManager extends de.Keyle.MyPet.api.util.CompatManager {
    public void init() {
        MyPetApi.getServiceManager().registerService(EggIconService.class);
    }
}

package de.Keyle.MyPet.entity.info;

import de.Keyle.MyPet.api.entity.MyPetInfo;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.exceptions.PetTypeNotFoundException;
import org.bukkit.entity.EntityType;

/**
 * Concrete plugin-side implementation of {@link MyPetInfo}.
 */
public class MyPetInfoImpl extends MyPetInfo {

    @Override
    public boolean isLeashableEntityType(EntityType bukkitType) {
        try {
            MyPetType type = MyPetType.byEntityTypeName(bukkitType.name());
            return type != null;
        } catch (PetTypeNotFoundException e) {
            return false;
        }
    }
}

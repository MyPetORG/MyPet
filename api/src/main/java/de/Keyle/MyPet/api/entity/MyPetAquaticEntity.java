package de.Keyle.MyPet.api.entity;

import de.Keyle.MyPet.api.Configuration;

/**
 * Marker for pet types whose underlying vanilla mob is an aquatic creature —
 * implies MyPet's movement layer should swim them in water and that
 * out-of-water survival logic (e.g., dryout damage cancellation) may apply.
 * Read at runtime by {@link MyPetType#isAquaticPet()} via
 * {@code Class.isAssignableFrom}.
 *
 * <p>The {@link #canSwim()} default consults the per-pet preference loaded
 * from {@code MyPet.Pets.<Type>.CanSwim} in {@code pet-config.yml}. The YAML
 * row is auto-registered for every type that implements this marker — adding
 * a new aquatic pet only requires implementing this interface.
 */
public interface MyPetAquaticEntity extends MyPet {

    default boolean canSwim() {
        return Configuration.MyPet.canSwim(getPetType());
    }
}

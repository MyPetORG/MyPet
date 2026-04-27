package de.Keyle.MyPet.api.entity;

import de.Keyle.MyPet.api.Configuration;

/**
 * Marker for pet types that slow-fall instead of dropping like a stone — e.g.,
 * Chicken, or any flying pet whose rider should drift down rather than plummet
 * when flight is disabled. Read at runtime via {@code Class.isAssignableFrom}.
 *
 * <p>The {@link #canGlide()} default consults the per-pet preference loaded
 * from {@code MyPet.Pets.<Type>.CanGlide} in {@code pet-config.yml}. The YAML
 * row is auto-registered for every type that implements this marker.
 *
 * <p>{@link MyPetFlyingEntity} extends this marker, so every flying pet is
 * also a gliding pet — necessary so a rider on a fly-disabled mount drifts
 * down instead of free-falling.
 */
public interface MyPetGlidingEntity extends MyPet {

    default boolean canGlide() {
        return Configuration.MyPet.canGlide(getPetType());
    }
}

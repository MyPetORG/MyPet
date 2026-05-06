package de.Keyle.MyPet.api.entity;

import de.Keyle.MyPet.api.Configuration;

/**
 * Marker for pet types whose underlying vanilla mob can swim — implies
 * MyPet's movement layer should treat the pet as a swimmer when it's in
 * water (aquatic follow path, no float goal). Read at runtime by
 * {@link PetType#isSwimmingPet()} via {@code Class.isAssignableFrom}.
 *
 * <p>This is the swim-physics base; concrete pets implement one of the two
 * sub-markers depending on their out-of-water survival semantics:
 * <ul>
 *   <li>{@link MyPetAquaticEntity} — water-breathers that suffocate on land
 *       (fish, squids, guardians, dolphin, tadpole)</li>
 *   <li>{@link MyPetAmphibiousEntity} — pets that swim but also survive on
 *       land indefinitely (axolotl, drowned, frog, turtle)</li>
 * </ul>
 *
 * <p>The {@link #canSwim()} default consults the per-pet preference loaded
 * from {@code MyPet.Pets.<Type>.CanSwim} in {@code pet-config.yml}. The YAML
 * row is auto-registered for every type that implements this marker — adding
 * a new swimming pet only requires implementing one of the sub-markers.
 *
 * <p>Mirrors the {@link MyPetGlidingEntity} / {@link MyPetFlyingEntity}
 * split for airborne pets: a swim-base (this) plus a stricter sub-marker
 * for the survival-affecting variant.
 */
public interface MyPetSwimmingEntity extends MyPet {

    default boolean canSwim() {
        return Configuration.MyPet.canSwim(getPetType());
    }
}

package de.Keyle.MyPet.api.entity;

import de.Keyle.MyPet.api.Configuration;

/**
 * Marker for pet types whose underlying vanilla mob naturally flies — implies
 * MyPet's AI/movement layer should treat this pet as airborne (no gravity,
 * flight pathing, no float goal). Read at runtime by
 * {@link PetType#isFlyingPet()} via {@code Class.isAssignableFrom}.
 *
 * <p>The {@link #canFly()} default consults the per-pet preference loaded
 * from {@code MyPet.Pets.<Type>.CanFly} in {@code pet-config.yml}. The YAML
 * row is auto-registered for every type that implements this marker — adding
 * a new flying pet only requires implementing this interface.
 *
 * <p>Extends {@link MyPetGlidingEntity} because every flying pet must also
 * glide: when an admin disables flight, a ridden pet still needs to slow-fall
 * so the rider doesn't plummet. The inherited {@link #canGlide()} reads its
 * own {@code CanGlide} config row.
 */
public interface MyPetFlyingEntity extends MyPetGlidingEntity {

    default boolean canFly() {
        return Configuration.MyPet.canFly(getPetType());
    }
}

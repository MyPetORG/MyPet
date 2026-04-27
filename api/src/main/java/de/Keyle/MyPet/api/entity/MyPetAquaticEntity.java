package de.Keyle.MyPet.api.entity;

/**
 * Marker for pet types whose underlying vanilla mob is an aquatic creature —
 * implies MyPet's movement layer should swim them in water and that
 * out-of-water survival logic (e.g., dryout damage cancellation) may apply.
 * Read at runtime by {@link MyPetType#isAquaticPet()} via
 * {@code Class.isAssignableFrom}.
 */
public interface MyPetAquaticEntity extends MyPet {
}

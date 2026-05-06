package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.entity.Entity;

import java.util.Optional;

import static de.Keyle.MyPet.MyPetApi.getPetManager;

/**
 * Shared guard helpers for pet-related event listeners.
 *
 * <p>Replaces the repeated null-check + marker-check + manager-lookup idiom
 * that previously appeared in every handler of {@code MyPetEntityListener}.
 */
public final class PetListenerGuards {

    private PetListenerGuards() {}

    /**
     * Returns the {@link MyPet} for the given entity if it is a marked,
     * non-null pet entity with a live pet object in the manager.
     *
     * <p>Handles the defensive null-check against broken plugin events
     * (e.g. EnchantmentAPI sending events with null entities).
     */
    @SuppressWarnings("ConstantConditions")
    public static Optional<MyPet> markedPet(Entity entity) {
        if (entity == null) return Optional.empty();
        if (!PetEntityMarker.isMarked(entity)) return Optional.empty();
        return Optional.ofNullable(getPetManager().getMyPetFromEntity(entity));
    }
}
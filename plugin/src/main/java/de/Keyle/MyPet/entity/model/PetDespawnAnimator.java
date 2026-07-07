/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
 *
 * MyPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.entity.model;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Mob;

import java.util.OptionalLong;

/**
 * Plays the despawn animation and delays the host's removal until it finishes, for the
 * safely-delayable despawn paths (store / remove). The backing {@link Pet} is already
 * despawned by the caller; this only governs the detached host mob, which finishes its
 * animation as an ordinary (unmarked) vanilla mob and then self-removes.
 */
public final class PetDespawnAnimator {

    private PetDespawnAnimator() {
    }

    /**
     * Tries to play the despawn animation on {@code entity} and schedule its removal after the
     * animation's length. Returns {@code true} when it has claimed responsibility for removing
     * {@code entity} — the caller must then NOT remove it itself. Returns {@code false}
     * (caller removes normally) unless ALL hold: a model is present, the owner is online, and
     * the entity's region is owned (so the delayed removal can be scheduled safely on Folia).
     *
     * <p>Must be called while {@code pet.getBukkitEntity()} still references {@code entity}
     * (before the caller nulls it), since the model lookup goes through the pet.
     */
    public static boolean tryAnimate(Pet pet, Mob entity) {
        if (pet == null || entity == null) {
            return false;
        }
        if (pet.getOwner() == null || !pet.getOwner().isOnline()) {
            return false;
        }
        if (!Bukkit.isOwnedByCurrentRegion(entity)) {
            return false;
        }
        if (!PetModelService.hasActiveModel(pet)) {
            return false;
        }
        OptionalLong delay = PetModelService.playDespawn(pet);
        if (delay.isEmpty()) {
            return false; // no model after all -> caller removes now
        }
        // Detach the pet marker so no listener treats the orphaned, animating mob as a live
        // pet during the delay window (its backing Pet is already despawned, entity nulled).
        PetEntityMarker.unmark(entity);
        Runnable removeEntity = () -> {
            try {
                entity.remove();
            } catch (Throwable ignored) {
                // entity may already be gone (chunk unload / shutdown) — vanilla removed it
            }
        };
        // The retired callback fires instead of the task when the scheduler can never run it (region
        // unload, plugin disable/shutdown) — without it the host would be left animating forever,
        // hidden but never removed. Fall back to the same removal so it can't be stranded either way.
        entity.getScheduler().runDelayed(MyPetApi.getPlugin(), task -> removeEntity.run(), removeEntity, delay.getAsLong());
        return true;
    }
}

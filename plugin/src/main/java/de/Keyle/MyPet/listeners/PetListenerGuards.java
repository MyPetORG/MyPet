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

package de.Keyle.MyPet.listeners;

import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.entity.Entity;

import java.util.Optional;

import static de.Keyle.MyPet.MyPetApi.getPetManager;

/**
 * Shared guard helpers for pet-related event listeners.
 */
public final class PetListenerGuards {

    private PetListenerGuards() {}

    /**
     * Returns the {@link Pet} for the given entity if it is a marked,
     * non-null pet entity with a live pet object in the manager.
     *
     * <p>Handles the defensive null-check against broken plugin events
     * (e.g. EnchantmentAPI sending events with null entities).
     */
    public static Optional<Pet> markedPet(Entity entity) {
        if (entity == null) return Optional.empty();
        if (!PetEntityMarker.isMarked(entity)) return Optional.empty();
        return Optional.ofNullable(getPetManager().getPetFromEntity(entity));
    }
}
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

package de.Keyle.MyPet.api.lifecycle;

import de.Keyle.MyPet.api.entity.Pet;

import java.util.function.Consumer;

/**
 * Per-pet-type runtime hook fired when a pet spawns and despawns. Used by
 * pet-specific visual controllers, schedulers, and AI suppressors whose
 * activation/cleanup needs to run alongside the Bukkit entity lifecycle.
 *
 * <p>Declared as a static field on the matching {@code PetXxx} class:
 *
 * <pre>{@code
 * public static final PetLifecycleHook LIFECYCLE_HOOK = new PetLifecycleHook(
 *     "Creaking",
 *     ActivationSuppressor::startForPet,
 *     ActivationSuppressor::stopForPet
 * );
 * }</pre>
 *
 * <p>Construction self-registers with {@link PetLifecycleHookRegistry}, so
 * shared spawn/despawn code in {@code PetImpl}, {@code VanillaMobSpawner},
 * and {@code PetManager} iterates hooks via {@link PetLifecycleHookRegistry#forPet}
 * without naming any specific pet type.
 */
public final class PetLifecycleHook {

    private final String petType;
    private final Consumer<Pet> onSpawn;
    private final Consumer<Pet> onDespawn;

    public PetLifecycleHook(String petType, Consumer<Pet> onSpawn, Consumer<Pet> onDespawn) {
        this.petType = petType;
        this.onSpawn = onSpawn;
        this.onDespawn = onDespawn;
        PetLifecycleHookRegistry.register(this);
    }

    public String petType() {
        return petType;
    }

    public void onSpawn(Pet pet) {
        onSpawn.accept(pet);
    }

    public void onDespawn(Pet pet) {
        onDespawn.accept(pet);
    }
}

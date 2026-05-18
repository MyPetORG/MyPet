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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.PetType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central registry of every {@link PetLifecycleHook} declared as a static
 * field on a pet class. Populated as a side effect of each hook's
 * constructor, which fires during the pet class's {@code <clinit>}.
 *
 * <p>{@link #forPet(Pet)} returns the hooks registered for the given pet's
 * type — shared spawn/despawn code in {@code PetImpl}, {@code VanillaMobSpawner},
 * and {@code PetManager} iterates the result without naming any specific pet
 * type. Adding a new pet-specific lifecycle hook is a one-line declaration
 * on the pet class; no shared file is touched.
 *
 * <p>{@link #ensurePetsLoaded()} force-initializes every registered pet
 * class via {@code Class.forName(true)} so the registry is fully populated
 * before the first spawn-side iteration.
 */
public final class PetLifecycleHookRegistry {

    private PetLifecycleHookRegistry() {}

    private static final Map<String, List<PetLifecycleHook>> HOOKS_BY_TYPE = new ConcurrentHashMap<>();
    private static volatile boolean petsLoaded = false;

    /**
     * Force-initializes every registered pet class via {@code Class.forName(true)}
     * so each one's static {@link PetLifecycleHook} field initializers fire
     * and register with this registry. Idempotent; runs once per JVM.
     */
    public static synchronized void ensurePetsLoaded() {
        if (petsLoaded) return;
        for (PetType petType : PetType.values()) {
            try {
                Class.forName(petType.getPetClass().getName(), true,
                        petType.getPetClass().getClassLoader());
            } catch (Throwable t) {
                MyPetApi.getLogger().warning(
                        "PetLifecycleHookRegistry: failed to load pet class for "
                                + petType.name() + ": " + t.getClass().getSimpleName()
                                + ": " + t.getMessage());
            }
        }
        petsLoaded = true;
    }

    /** Adds {@code hook} to the registry. Called from the hook's constructor. */
    static void register(PetLifecycleHook hook) {
        HOOKS_BY_TYPE.computeIfAbsent(hook.petType(), k -> new CopyOnWriteArrayList<>())
                .add(hook);
    }

    /**
     * Returns every lifecycle hook registered for {@code pet}'s type, in
     * registration order. Returns an empty list when no hook is registered.
     */
    public static List<PetLifecycleHook> forPet(Pet pet) {
        ensurePetsLoaded();
        return HOOKS_BY_TYPE.getOrDefault(pet.getPetType().name(), List.of());
    }
}

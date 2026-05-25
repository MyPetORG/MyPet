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

package de.Keyle.MyPet.api.brain;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.PetType;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central registry of every {@link PetBrainBehaviorRemoval} declared as a
 * static field on a pet class. Populated as a side effect of each
 * declaration's constructor, which fires during the pet class's
 * {@code <clinit>}.
 *
 * <p>{@link #behaviorNamesFor(Pet)} returns the union of every brain-behavior
 * simple class name registered for the pet's type. {@code PetGoalInstaller}
 * consults this at spawn time and asks the reflection helper to strip the
 * matching behaviors — parallel to the {@code removeAllGoals} sweep that
 * runs immediately before it. The instaler names no specific pet type.
 *
 * <p>Adding a new strip declaration is a one-line {@link PetBrainBehaviorRemoval}
 * static field on the relevant {@code PetXxx} class. The reflection helper
 * and the instaler are untouched.
 *
 * <p>{@link #ensurePetsLoaded()} force-initializes every registered pet
 * class via {@code Class.forName(true)} so the registry is fully populated
 * before the first spawn-side iteration. Same shape as the other
 * pet-class registries in this package family
 * ({@code PetLifecycleHookRegistry}, {@code ConfigKeyRegistry},
 * {@code PetBehaviorRegistry}).
 */
public final class PetBrainBehaviorRemovalRegistry {

    private PetBrainBehaviorRemovalRegistry() {}

    private static final Map<String, List<PetBrainBehaviorRemoval>> BY_TYPE = new ConcurrentHashMap<>();
    private static volatile boolean petsLoaded = false;

    /**
     * Force-initializes every registered pet class via {@code Class.forName(true)}
     * so each one's static {@link PetBrainBehaviorRemoval} field initializers
     * fire and register with this registry. Idempotent; runs once per JVM.
     */
    public static synchronized void ensurePetsLoaded() {
        if (petsLoaded) return;
        for (PetType petType : PetType.values()) {
            try {
                Class.forName(petType.getPetClass().getName(), true,
                        petType.getPetClass().getClassLoader());
            } catch (Throwable t) {
                MyPetApi.getLogger().warning(
                        "PetBrainBehaviorRemovalRegistry: failed to load pet class for "
                                + petType.name() + ": " + t.getClass().getSimpleName()
                                + ": " + t.getMessage());
            }
        }
        petsLoaded = true;
    }

    /** Adds {@code removal} to the registry. Called from the handle's constructor. */
    static void register(PetBrainBehaviorRemoval removal) {
        BY_TYPE.computeIfAbsent(removal.petType(), k -> new CopyOnWriteArrayList<>())
                .add(removal);
    }

    /**
     * Returns the union of every brain-behavior simple class name registered
     * for {@code pet}'s type. Empty set when no removals are declared.
     * Preserves declaration order across handles for stable log output.
     */
    public static Set<String> behaviorNamesFor(Pet pet) {
        ensurePetsLoaded();
        List<PetBrainBehaviorRemoval> declarations = BY_TYPE.getOrDefault(
                pet.getPetType().name(), List.of());
        if (declarations.isEmpty()) return Set.of();
        Set<String> union = new LinkedHashSet<>();
        for (PetBrainBehaviorRemoval declaration : declarations) {
            union.addAll(declaration.behaviorClassNames());
        }
        return union;
    }
}

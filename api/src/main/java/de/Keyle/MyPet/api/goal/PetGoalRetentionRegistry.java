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

package de.Keyle.MyPet.api.goal;

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
 * Central registry of every {@link PetGoalRetention} declared as a static
 * field on a pet class. Populated as a side effect of each declaration's
 * constructor, which fires during the pet class's {@code <clinit>}.
 *
 * <p>{@link #goalNamesFor(Pet)} returns the union of every vanilla goal-key
 * name registered for the pet's type. {@code PetGoalInstaller} consults this
 * at spawn time — if the set is empty (no retention declared), it falls back
 * to the default {@code removeAllGoals(mob)} behavior; otherwise it iterates
 * the live mob's goals and removes only those whose key isn't in the set.
 *
 * <p>Adding a new retention is a one-line {@link PetGoalRetention} static
 * field on the relevant {@code PetXxx} class.
 *
 * <p>{@link #ensurePetsLoaded()} force-initializes every registered pet
 * class via {@code Class.forName(true)} so the registry is fully populated
 * before the first spawn-side iteration. Same shape as the other
 * pet-class registries in this package family.
 */
public final class PetGoalRetentionRegistry {

    private PetGoalRetentionRegistry() {}

    private static final Map<String, List<PetGoalRetention>> BY_TYPE = new ConcurrentHashMap<>();
    private static volatile boolean petsLoaded = false;

    /**
     * Force-initializes every registered pet class via {@code Class.forName(true)}
     * so each one's static {@link PetGoalRetention} field initializers fire
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
                        "PetGoalRetentionRegistry: failed to load pet class for "
                                + petType.name() + ": " + t.getClass().getSimpleName()
                                + ": " + t.getMessage());
            }
        }
        petsLoaded = true;
    }

    /** Adds {@code retention} to the registry. Called from the handle's constructor. */
    static void register(PetGoalRetention retention) {
        BY_TYPE.computeIfAbsent(retention.petType(), k -> new CopyOnWriteArrayList<>())
                .add(retention);
    }

    /**
     * Returns the union of every vanilla goal-key name registered for
     * {@code pet}'s type. Empty set when no retentions are declared.
     */
    public static Set<String> goalNamesFor(Pet pet) {
        ensurePetsLoaded();
        List<PetGoalRetention> declarations = BY_TYPE.getOrDefault(
                pet.getPetType().name(), List.of());
        if (declarations.isEmpty()) return Set.of();
        Set<String> union = new LinkedHashSet<>();
        for (PetGoalRetention declaration : declarations) {
            union.addAll(declaration.goalKeyNames());
        }
        return union;
    }
}

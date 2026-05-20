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

package de.Keyle.MyPet.api.entity.leashing;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.PetType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry of every {@link WildAngerCheck} declared as a static
 * field on a pet class. Populated as a side effect of each check's
 * constructor, which fires during the pet class's {@code <clinit>}.
 *
 * <p>{@link #forEntity(LivingEntity)} returns the check registered for the
 * given entity's species, or {@link Optional#empty()} if no pet class
 * declares one. {@code AngryFlag} iterates the result without naming any
 * specific pet type; adding a new angerable species is a one-line static
 * field on its pet class.
 *
 * <p>{@link #ensurePetsLoaded()} force-initializes every registered pet
 * class via {@code Class.forName(true)} so the registry is fully populated
 * before the first lookup.
 */
public final class WildAngerCheckRegistry {

    private WildAngerCheckRegistry() {}

    private static final Map<Class<?>, WildAngerCheck<?>> CHECKS_BY_MOB_CLASS = new ConcurrentHashMap<>();
    private static volatile boolean petsLoaded = false;

    /**
     * Force-initializes every registered pet class via {@code Class.forName(true)}
     * so each one's static {@link WildAngerCheck} field initializers fire
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
                        "WildAngerCheckRegistry: failed to load pet class for "
                                + petType.name() + ": " + t.getClass().getSimpleName()
                                + ": " + t.getMessage());
            }
        }
        petsLoaded = true;
    }

    /** Adds {@code check} to the registry. Called from the check's constructor. */
    static void register(WildAngerCheck<?> check) {
        WildAngerCheck<?> previous = CHECKS_BY_MOB_CLASS.put(check.mobClass(), check);
        if (previous != null) {
            MyPetApi.getLogger().warning(
                    "WildAngerCheckRegistry: duplicate WildAngerCheck for "
                            + check.mobClass().getSimpleName() + "; second declaration wins");
        }
    }

    /**
     * Returns the anger check registered for {@code entity}'s species, or
     * {@link Optional#empty()} if no pet class declares one (either the
     * species isn't a MyPet pet type, or its pet class doesn't expose anger
     * semantics).
     */
    public static Optional<WildAngerCheck<?>> forEntity(LivingEntity entity) {
        ensurePetsLoaded();
        Class<? extends Entity> apiClass = entity.getType().getEntityClass();
        if (apiClass == null) return Optional.empty();
        return Optional.ofNullable(CHECKS_BY_MOB_CLASS.get(apiClass));
    }
}

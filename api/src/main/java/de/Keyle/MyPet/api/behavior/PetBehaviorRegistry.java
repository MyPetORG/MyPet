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

package de.Keyle.MyPet.api.behavior;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.PetType;
import org.bukkit.event.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Central registry of every {@link PetBehavior} declared as a static field
 * on a pet class. Populated as a side effect of each behavior's factory
 * call ({@link PetBehaviorHelpers#onPetInteract}, etc.) which fires during
 * the pet class's {@code <clinit>}.
 *
 * <p>Plugin-side {@code PetBehaviorDispatcher} reads this at startup and
 * registers one Bukkit {@code EventExecutor} per behavior, routing live
 * events through {@link PetBehavior#handler} after pet-type filtering.
 *
 * <p>{@link #ensurePetsLoaded()} force-initializes every registered pet
 * class via {@code Class.forName(true)} so the registry is fully populated
 * before the dispatcher iterates.
 */
public final class PetBehaviorRegistry {

    private PetBehaviorRegistry() {}

    private static final List<PetBehavior<?>> BEHAVIORS = new CopyOnWriteArrayList<>();
    private static volatile boolean petsLoaded = false;
    private static volatile Consumer<PetBehavior<?>> dispatchHook;

    /**
     * Force-initializes every registered pet class via {@code Class.forName(true)}
     * so each one's static {@link PetBehavior} field initializers fire and
     * register with this registry. Idempotent; runs once per JVM.
     */
    public static synchronized void ensurePetsLoaded() {
        if (petsLoaded) return;
        for (PetType petType : PetType.values()) {
            try {
                Class.forName(petType.getPetClass().getName(), true,
                        petType.getPetClass().getClassLoader());
            } catch (Throwable t) {
                MyPetApi.getLogger().warning(
                        "PetBehaviorRegistry: failed to load pet class for "
                                + petType.name() + ": " + t.getClass().getSimpleName()
                                + ": " + t.getMessage());
            }
        }
        petsLoaded = true;
    }

    /**
     * Adds {@code behavior} to the registry and immediately invokes the
     * {@linkplain #setDispatchHook(Consumer) dispatch hook} if one is set.
     * Late registrations (e.g. third-party pet types registered in their
     * own {@code onEnable} after MyPet's dispatcher has already run) are
     * wired without requiring a refresh call.
     */
    static void register(PetBehavior<?> behavior) {
        BEHAVIORS.add(behavior);
        Consumer<PetBehavior<?>> hook = dispatchHook;
        if (hook != null) {
            hook.accept(behavior);
        }
    }

    /**
     * Installs a callback fired for every subsequent {@link #register}.
     * {@code PetBehaviorDispatcher} sets this after wiring the initial set,
     * so behaviors registered later (third-party plugin {@code onEnable})
     * get a Bukkit executor too.
     *
     * <p>The hook is responsible for its own deduplication if it also
     * iterates {@link #all()} — a behavior registered during the dispatcher's
     * initial pass may surface here too if registration happens concurrently.
     */
    public static void setDispatchHook(Consumer<PetBehavior<?>> hook) {
        dispatchHook = hook;
    }

    /**
     * Returns a snapshot of every registered behavior. Iteration order
     * matches registration order. Used by the plugin-side dispatcher at
     * startup to wire each behavior to a Bukkit {@code EventExecutor}.
     */
    public static List<PetBehavior<?>> all() {
        ensurePetsLoaded();
        return new ArrayList<>(BEHAVIORS);
    }

    /**
     * Returns the set of distinct event classes any registered behavior
     * cares about. Convenience for dispatchers that want to iterate
     * event-class-keyed rather than behavior-keyed.
     */
    public static List<Class<? extends Event>> eventClasses() {
        ensurePetsLoaded();
        List<Class<? extends Event>> seen = new ArrayList<>();
        for (PetBehavior<?> b : BEHAVIORS) {
            if (!seen.contains(b.eventClass())) {
                seen.add(b.eventClass());
            }
        }
        return seen;
    }
}

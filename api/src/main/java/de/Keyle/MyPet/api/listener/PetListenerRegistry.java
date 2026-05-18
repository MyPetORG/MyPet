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

package de.Keyle.MyPet.api.listener;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.PetType;
import lombok.Setter;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Central registry of every Bukkit {@link Listener} declared as a nested
 * static class on a pet class. Populated as a side effect of each pet
 * class's {@code <clinit>} which assigns a {@code Supplier<Listener>}
 * static field via {@link #register(Supplier)}.
 *
 * <p>The plugin-side {@code PetListeners.registerAll} iterates this
 * registry after wiring its cross-cutting listeners and registers each
 * pet-nested listener with Bukkit. A supplier returning {@code null}
 * signals "do not register on this server" — used by pet-specific
 * listeners that need a Minecraft version gate (e.g., Creaking on 1.21.4+).
 *
 * <p>{@link #ensurePetsLoaded()} force-initializes every registered pet
 * class via {@code Class.forName(true)} so the registry is fully
 * populated before {@link #all()} returns.
 */
public final class PetListenerRegistry {

    private PetListenerRegistry() {}

    private static final List<Supplier<Listener>> SUPPLIERS = new CopyOnWriteArrayList<>();
    private static volatile boolean petsLoaded = false;

    /**
     * Callback fired for every subsequent {@link #register}.
     * {@code PetListeners.registerAll} sets this after the initial sweep
     * so third-party pet types declared in their own {@code onEnable} get
     * their listeners wired without a refresh call.
     */
    @Setter
    private static volatile Consumer<Supplier<Listener>> dispatchHook;

    /**
     * Force-initializes every registered pet class via {@code Class.forName(true)}
     * so each one's static {@code Supplier<Listener>} field initializers fire
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
                        "PetListenerRegistry: failed to load pet class for "
                                + petType.name() + ": " + t.getClass().getSimpleName()
                                + ": " + t.getMessage());
            }
        }
        petsLoaded = true;
    }

    /**
     * Records a listener supplier and returns it so the caller can assign
     * the result to a {@code public static final Supplier<Listener>} field.
     * The supplier is invoked at registration time — return {@code null}
     * from the supplier to skip registration (e.g., version-gated listeners).
     *
     * <p>If a {@linkplain #setDispatchHook(Consumer) dispatch hook} is
     * installed, the new supplier is also passed to it so late
     * registrations (third-party pet types declared after MyPet's
     * registerAll has already run) are wired without a refresh call.
     */
    public static Supplier<Listener> register(Supplier<Listener> supplier) {
        SUPPLIERS.add(supplier);
        Consumer<Supplier<Listener>> hook = dispatchHook;
        if (hook != null) {
            hook.accept(supplier);
        }
        return supplier;
    }

    /**
     * Returns a snapshot of every registered supplier in registration
     * order. Iteration follows {@link PetType#values()} class-loading
     * order (deterministic across JVMs).
     */
    public static List<Supplier<Listener>> all() {
        ensurePetsLoaded();
        return new ArrayList<>(SUPPLIERS);
    }
}

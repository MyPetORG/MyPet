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

import de.Keyle.MyPet.api.listener.PetListenerRegistry;
import de.Keyle.MyPet.behavior.PetBehaviorDispatcher;
import de.Keyle.MyPet.entity.ai.attack.PetProjectileHitListener;
import de.Keyle.MyPet.entity.ai.target.PetDamageTracker;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Registers MyPet's bundled Bukkit event listeners with Paper's plugin manager.
 *
 * <p>Two sources are wired:
 * <ul>
 *   <li>The static {@link #LISTENERS} list — cross-cutting listeners that
 *       apply to every pet (PvP gating, despawn cleanup, leash flags, etc.).</li>
 *   <li>{@link PetListenerRegistry#all()} — pet-nested {@link Listener}s
 *       declared as {@code public static final Supplier<Listener>} fields
 *       on individual {@code PetXxx} classes. Each pet type owns its own
 *       species-specific event handling.</li>
 * </ul>
 *
 * <p><b>Order matters within an {@link org.bukkit.event.EventPriority} bucket.</b>
 * Bukkit invokes handlers in registration order when priorities are equal; the
 * {@link #LISTENERS} list preserves the historical ordering. Pet-nested
 * listeners register after the static list in {@link PetListenerRegistry}
 * registration order (deterministic via {@code PetType.values()} iteration).
 * After the initial sweep, a dispatch hook is installed so late
 * registrations (third-party pet types declared in their own {@code onEnable})
 * get wired without a refresh call.</p>
 *
 * <p>Invoked once during plugin enable, after all services have been activated.</p>
 */
public final class PetListeners {

    private static final List<Supplier<Listener>> LISTENERS = List.of(
            PlayerListener::new,
            VehicleListener::new,
            EntityListener::new,
            LevelListener::new,
            WorldListener::new,
            RideInteractListener::new,
            PetMountGateListener::new,
            PetSaddleGateListener::new,
            PetDamageTracker::new,
            PetProjectileHitListener::new,
            PetInteractionListener::new,
            PetInteractionGateListener::new,
            PetBucketGateListener::new,
            PetEnvironmentListener::new,
            PetLightningStrikeListener::new,
            PetZombificationListener::new,
            PetInfoOnLeashListener::new,
            PetSurvivalListener::new,
            PetXpAttributionListener::new,
            PetPvPListener::new,
            PetSkillTriggerListener::new,
            PetDeathListener::new,
            PetDespawnListener::new,
            PetDropListener::new
    );

    private PetListeners() {
    }

    /**
     * Wires every cross-cutting listener from {@link #LISTENERS}, then every
     * pet-nested listener from {@link PetListenerRegistry}. Installs a
     * dispatch hook so suppliers registered after this call (third-party
     * pet types declared in their own {@code onEnable}) are wired
     * automatically.
     *
     * @param plugin the plugin to associate registrations with; events fire only while this
     *               plugin is enabled
     */
    public static void registerAll(@NotNull Plugin plugin) {
        PluginManager pm = plugin.getServer().getPluginManager();
        for (Supplier<Listener> listener : LISTENERS) {
            pm.registerEvents(listener.get(), plugin);
        }
        // Golden-dandelion age-lock toggle (Paper 26.1+) is wired reflectively —
        // its event class is absent from the 1.21.x API, so it self-skips there.
        PetAgeLockListener.register(plugin);
        // Dedup set covers the narrow race between the initial sweep and any
        // late registration that fires the dispatch hook concurrently.
        Set<Supplier<Listener>> wired = ConcurrentHashMap.newKeySet();
        PetListenerRegistry.setDispatchHook(supplier -> {
            if (wired.add(supplier)) {
                Listener instance = supplier.get();
                if (instance != null) pm.registerEvents(instance, plugin);
            }
        });
        for (Supplier<Listener> supplier : PetListenerRegistry.all()) {
            if (wired.add(supplier)) {
                Listener instance = supplier.get();
                if (instance != null) pm.registerEvents(instance, plugin);
            }
        }
        // PetBehaviorDispatcher registers one Bukkit executor per PetBehavior declared
        // on individual PetXxx classes. Runs after the static + nested listeners so
        // @EventHandler methods preserve their relative ordering; per-pet behaviors
        // fire alongside.
        PetBehaviorDispatcher.registerAll(plugin);
    }
}

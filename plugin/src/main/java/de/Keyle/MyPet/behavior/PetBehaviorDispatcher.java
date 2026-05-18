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

package de.Keyle.MyPet.behavior;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.behavior.PetBehavior;
import de.Keyle.MyPet.api.behavior.PetBehaviorRegistry;
import de.Keyle.MyPet.api.entity.Pet;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bukkit-side bridge that wires every {@link PetBehavior} in
 * {@link PetBehaviorRegistry} to a real Bukkit {@code EventExecutor}.
 *
 * <p>Called once during plugin enable. For each registered behavior, this
 * registers an executor on the matching event class at the behavior's
 * priority. When Bukkit fires the event, the executor:
 *
 * <ol>
 *   <li>Pulls the relevant entity out of the event via
 *       {@link PetBehavior#entityExtractor}.</li>
 *   <li>Looks up the entity in {@link MyPetApi#getPetManager}. Skip if it's
 *       not a managed pet.</li>
 *   <li>Compares the pet's type name to {@link PetBehavior#petType}.
 *       Skip if no match.</li>
 *   <li>Invokes the typed {@link PetBehavior#handler} with the event,
 *       pet, and Bukkit {@link Mob}.</li>
 * </ol>
 *
 * <p>Handler exceptions are caught and logged so a buggy behavior on one
 * pet type can't break unrelated event handling on the same event class.
 */
public final class PetBehaviorDispatcher implements Listener {

    private PetBehaviorDispatcher() {}

    /**
     * Registers a Bukkit executor for every behavior in
     * {@link PetBehaviorRegistry}, then installs a dispatch hook so any
     * behavior registered later (third-party plugin {@code onEnable}
     * declaring a static {@code PetBehavior} field on a pet class) is
     * wired without requiring a refresh call.
     *
     * <p>{@link PetBehaviorRegistry#all()} transitively force-loads every
     * pet class via {@code Class.forName(true)}, so each {@code PetXxx}'s
     * static {@code PetBehavior<?>} field initializers fire and register
     * before we iterate.
     */
    public static void registerAll(Plugin plugin) {
        PetBehaviorDispatcher self = new PetBehaviorDispatcher();
        PluginManager pm = plugin.getServer().getPluginManager();
        // Dedup set covers the narrow race window between this initial
        // sweep and any registration that fires the hook concurrently.
        Set<PetBehavior<?>> wired = ConcurrentHashMap.newKeySet();
        PetBehaviorRegistry.setDispatchHook(behavior -> {
            if (wired.add(behavior)) {
                registerOne(self, plugin, pm, behavior);
            }
        });
        for (PetBehavior<?> b : PetBehaviorRegistry.all()) {
            if (wired.add(b)) {
                registerOne(self, plugin, pm, b);
            }
        }
    }

    private static <E extends Event> void registerOne(
            Listener listener, Plugin plugin, PluginManager pm, PetBehavior<E> behavior) {
        pm.registerEvent(
                behavior.eventClass(),
                listener,
                behavior.priority(),
                (l, event) -> {
                    if (behavior.eventClass().isInstance(event)) {
                        invoke(behavior, behavior.eventClass().cast(event));
                    }
                },
                plugin,
                behavior.ignoreCancelled());
    }

    private static <E extends Event> void invoke(PetBehavior<E> behavior, E event) {
        Entity entity = behavior.entityExtractor().apply(event);
        if (entity == null) return;
        // In disabled world groups, pets behave as vanilla mobs — skip
        // PetBehavior dispatch entirely so per-pet gating doesn't interfere
        // with vanilla physics in worlds where MyPet is turned off.
        if (WorldGroup.getGroupByWorld(entity.getWorld()).isDisabled()) return;
        Pet pet = MyPetApi.getPetManager().getPetFromEntity(entity);
        if (pet == null) return;
        // Match on the registered PetType name (consistent with
        // PetLifecycleHookRegistry.forPet). Using getSimpleName().substring(3)
        // would diverge for third-party pet classes that don't follow the
        // "Pet" prefix convention.
        if (!pet.getPetType().name().equals(behavior.petType())) return;
        if (!(entity instanceof Mob mob)) return;
        try {
            behavior.handler().accept(event, pet, mob);
        } catch (Throwable t) {
            MyPetApi.getLogger().warning(
                    "PetBehavior handler for " + behavior.petType() + "/"
                            + behavior.eventClass().getSimpleName() + " threw "
                            + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }
}

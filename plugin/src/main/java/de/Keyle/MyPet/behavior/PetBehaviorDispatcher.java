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
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Bukkit-side bridge that wires every {@link PetBehavior} in
 * {@link PetBehaviorRegistry} to a real Bukkit {@code EventExecutor}.
 *
 * <p>Called once during plugin enable. Behaviors sharing (event class,
 * priority, ignoreCancelled) share ONE Bukkit executor — hot events like
 * {@code EntityDamageByEntityEvent} carry several behaviors, and one
 * executor per behavior would multiply the per-event dispatch cost. When
 * Bukkit fires the event, the group executor:
 *
 * <ol>
 *   <li>Pulls the relevant entity out of the event via each behavior's
 *       {@link PetBehavior#entityExtractor} (extractors can differ within
 *       a group — victim vs. damager), memoizing consecutive lookups.</li>
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
        Map<GroupKey, List<PetBehavior<?>>> groups = new ConcurrentHashMap<>();
        // Dedup set covers the narrow race window between this initial
        // sweep and any registration that fires the hook concurrently.
        Set<PetBehavior<?>> wired = ConcurrentHashMap.newKeySet();
        PetBehaviorRegistry.setDispatchHook(behavior -> {
            if (wired.add(behavior)) {
                addToGroup(self, plugin, pm, groups, behavior);
            }
        });
        for (PetBehavior<?> b : PetBehaviorRegistry.all()) {
            if (wired.add(b)) {
                addToGroup(self, plugin, pm, groups, b);
            }
        }
    }

    /** One Bukkit executor per distinct registration shape. */
    private record GroupKey(Class<? extends Event> eventClass, EventPriority priority, boolean ignoreCancelled) {}

    private static void addToGroup(
            Listener listener, Plugin plugin, PluginManager pm,
            Map<GroupKey, List<PetBehavior<?>>> groups, PetBehavior<?> behavior) {
        GroupKey key = new GroupKey(behavior.eventClass(), behavior.priority(), behavior.ignoreCancelled());
        groups.computeIfAbsent(key, k -> {
            List<PetBehavior<?>> group = new CopyOnWriteArrayList<>();
            pm.registerEvent(
                    k.eventClass(),
                    listener,
                    k.priority(),
                    (l, event) -> {
                        if (k.eventClass().isInstance(event)) {
                            dispatch(group, event);
                        }
                    },
                    plugin,
                    k.ignoreCancelled());
            return group;
        }).add(behavior);
    }

    @SuppressWarnings("unchecked")
    private static void dispatch(List<PetBehavior<?>> group, Event event) {
        // Behaviors in a group may extract different entities from the same
        // event (onPetDamaged = victim, onPetDamages = damager), so the pet
        // is resolved per extracted entity, memoized for consecutive hits.
        Entity lastEntity = null;
        Pet lastPet = null;
        boolean worldChecked = false;
        for (PetBehavior<?> raw : group) {
            PetBehavior<Event> behavior = (PetBehavior<Event>) raw;
            Entity entity = behavior.entityExtractor().apply(event);
            if (entity == null) continue;
            Pet pet;
            if (entity == lastEntity) {
                pet = lastPet;
            } else {
                pet = MyPetApi.getPetManager().getPetFromEntity(entity);
                lastEntity = entity;
                lastPet = pet;
            }
            if (pet == null) continue;
            // In disabled world groups, pets behave as vanilla mobs — skip
            // dispatch entirely. Checked once per event, after the far more
            // selective pet check.
            if (!worldChecked) {
                if (WorldGroup.getGroupByWorld(entity.getWorld()).isDisabled()) return;
                worldChecked = true;
            }
            // Match on the registered PetType name (consistent with
            // PetLifecycleHookRegistry.forPet). Using getSimpleName().substring(3)
            // would diverge for third-party pet classes that don't follow the
            // "Pet" prefix convention.
            if (!pet.getPetType().name().equals(behavior.petType())) continue;
            if (!(entity instanceof Mob mob)) continue;
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
}

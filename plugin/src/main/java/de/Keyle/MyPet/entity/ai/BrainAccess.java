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

package de.Keyle.MyPet.entity.ai;

import de.Keyle.MyPet.MyPetApi;
import org.bukkit.entity.LivingEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reflective bridge to vanilla's brain system
 * ({@code net.minecraft.world.entity.ai.Brain}). Exposes two operations
 * MyPet needs and Bukkit doesn't surface:
 *
 * <ol>
 *   <li><b>Memory clearing</b> ({@link #clearAttackTarget}, {@link #clearWalkTarget})
 *       — per-tick erase of named brain memories. Used by per-pet
 *       {@link de.Keyle.MyPet.api.lifecycle.PetLifecycleHook}s as a fallback
 *       when behavior removal isn't appropriate (e.g., a memory is written
 *       from many producers).</li>
 *   <li><b>Behavior removal</b> ({@link #removeBehaviorsByClassName}) — one-shot
 *       at-spawn strip of named vanilla {@code BehaviorControl} instances
 *       from the brain's priority schedule. Used by
 *       {@link de.Keyle.MyPet.entity.spawn.PetGoalInstaller} immediately
 *       after {@code Bukkit.getMobGoals().removeAllGoals(mob)}, driven by
 *       per-pet {@link de.Keyle.MyPet.api.brain.PetBrainBehaviorRemoval}
 *       declarations. This is the preferred shape — it's parallel to the
 *       existing goal sweep, doesn't require per-tick scheduler overhead,
 *       and the behavior simply doesn't exist on the brain for the pet's
 *       lifetime.</li>
 * </ol>
 *
 * <p>Bukkit's {@link org.bukkit.entity.memory.MemoryKey} catalogue intentionally
 * excludes the brain memories MyPet needs to touch, and Bukkit has no
 * exposed surface for the brain's behavior schedule at all — so reflection
 * is the only available path. Mojang-mapped names are used directly
 * because Paper 1.20.5+ exposes the server jar under mojmap at runtime.
 * Same surgical-NMS-reflection pattern as
 * {@link de.Keyle.MyPet.entity.ai.movement.CubeMobMoveControlAccess} — a
 * deliberate exception to the codebase's "no NMS" stance for cases where
 * Bukkit doesn't expose the needed API.
 *
 * <p>Why memory-clear is kept alongside behavior-removal:
 * targeting and movement in 1.21.x brains often have many producers — e.g.
 * {@code WALK_TARGET} is written by {@code RandomStroll},
 * {@code SetWalkTargetFromLookTarget}, {@code AnimalPanic}, and species-
 * specific behaviors. When the goal is "neutralize this memory no matter
 * which behavior wrote it", clearing the memory each tick is cheaper than
 * enumerating every producer. When the goal is "this specific behavior is
 * misbehaving" (CopperGolem's {@code TransportItemsBetweenContainers}),
 * behavior removal is surgically exact.
 *
 * <p><b>Fail-soft:</b> if any lookup breaks (rename in a future MC version,
 * class loader oddity, etc.), a single warning is logged and subsequent
 * calls become no-ops. The brain-driven behaviors return; affected pet
 * classes carry their own additional guards (projectile-launch suppressors
 * on combat-relevant pets) so player safety doesn't degrade silently.
 */
public final class BrainAccess {

    private BrainAccess() {}

    private static volatile boolean initialized = false;
    private static volatile boolean available = false;
    private static volatile Method getHandleMethod;
    private static volatile Method getBrainMethod;
    private static volatile Method eraseMemoryMethod;
    private static volatile Field availableBehaviorsByPriorityField;
    private static volatile Class<?> gateBehaviorClass;
    private static volatile Field gateBehaviorBehaviorsField;
    private static volatile Field shufflingListEntriesField;
    private static volatile Method weightedEntryGetDataMethod;
    private static volatile Object attackTargetMemoryType;
    private static volatile Object walkTargetMemoryType;

    private static synchronized void tryInit(LivingEntity entity) {
        if (initialized) return;
        initialized = true;
        try {
            Class<?> craftEntity = Class.forName("org.bukkit.craftbukkit.entity.CraftEntity");
            Class<?> nmsLivingEntity = Class.forName("net.minecraft.world.entity.LivingEntity");
            Class<?> nmsBrain = Class.forName("net.minecraft.world.entity.ai.Brain");
            Class<?> nmsMemoryModuleType = Class.forName("net.minecraft.world.entity.ai.memory.MemoryModuleType");

            getHandleMethod = craftEntity.getMethod("getHandle");
            getBrainMethod = nmsLivingEntity.getMethod("getBrain");
            eraseMemoryMethod = nmsBrain.getMethod("eraseMemory", nmsMemoryModuleType);

            Field attackTargetField = nmsMemoryModuleType.getField("ATTACK_TARGET");
            attackTargetMemoryType = attackTargetField.get(null);

            Field walkTargetField = nmsMemoryModuleType.getField("WALK_TARGET");
            walkTargetMemoryType = walkTargetField.get(null);

            try {
                Field behaviorsField = nmsBrain.getDeclaredField("availableBehaviorsByPriority");
                behaviorsField.setAccessible(true);
                availableBehaviorsByPriorityField = behaviorsField;
            } catch (NoSuchFieldException renamed) {
                MyPetApi.getLogger().warning(
                        "BrainAccess: 'availableBehaviorsByPriority' field not found on Brain — vanilla brain-behavior removal disabled for this server (memory-clearing still works). PetBrainBehaviorRemoval declarations will be silently ignored until the field-name lookup is updated.");
                availableBehaviorsByPriorityField = null;
            }

            try {
                gateBehaviorClass = Class.forName("net.minecraft.world.entity.ai.behavior.GateBehavior");
                Field gateBehaviors = gateBehaviorClass.getDeclaredField("behaviors");
                gateBehaviors.setAccessible(true);
                gateBehaviorBehaviorsField = gateBehaviors;

                Class<?> shufflingList = Class.forName("net.minecraft.world.entity.ai.behavior.ShufflingList");
                Field entries = shufflingList.getDeclaredField("entries");
                entries.setAccessible(true);
                shufflingListEntriesField = entries;

                Class<?> weightedEntry = Class.forName("net.minecraft.world.entity.ai.behavior.ShufflingList$WeightedEntry");
                weightedEntryGetDataMethod = weightedEntry.getMethod("getData");
            } catch (Throwable composites) {
                MyPetApi.getLogger().warning(
                        "BrainAccess: composite-behavior reflection setup failed — behaviors nested inside RunOne / GateBehavior subclasses cannot be stripped (top-level behaviors still work). Cause: " + composites);
                gateBehaviorClass = null;
            }

            available = true;
        } catch (Throwable t) {
            MyPetApi.getLogger().warning(
                    "BrainAccess unavailable — brain-driven autonomous attacks/movement will not be suppressed at the brain level (projectile-launch suppressors still block actual damage on combat-relevant pets). Cause: " + t);
        }
    }

    /**
     * Erases the {@code ATTACK_TARGET} memory from the entity's brain.
     * Called every tick by per-pet lifecycle hooks (e.g.
     * {@code PetBreeze.AutonomousAttackSuppressor}) to prevent vanilla
     * brain behaviors from acting on a target picked by the nearest-target
     * sensor.
     *
     * <p>Brain sensors typically run before behaviors within a single
     * entity tick, so a per-tick clear may not always beat the sensor.
     * That's why each affected pet still has a projectile-launch-side
     * backup suppressor — together they cover both the windup
     * animation/sound (this method) and the actual damage (the launch
     * cancel).
     *
     * <p>Fail-soft: if the underlying reflection lookup ever breaks this
     * becomes a no-op.
     */
    public static void clearAttackTarget(LivingEntity entity) {
        if (!initialized) tryInit(entity);
        if (!available) return;
        try {
            Object handle = getHandleMethod.invoke(entity);
            Object brain = getBrainMethod.invoke(handle);
            eraseMemoryMethod.invoke(brain, attackTargetMemoryType);
        } catch (Throwable t) {
            // Don't spam logs — drop silently after a successful init.
        }
    }

    /**
     * Erases the {@code WALK_TARGET} memory from the entity's brain.
     * Called every tick by per-pet lifecycle hooks (e.g.
     * {@code PetCamel.WanderSuppressor}) to prevent the brain's
     * autonomous-movement pipeline from walking a pet around on its own.
     *
     * <p>This is the brain-level equivalent of stripping the random-stroll
     * goal from a vanilla {@code Mob}, except the camel's stroll lives as
     * a {@code Behavior<Camel>} on the brain and isn't reachable from
     * {@code Bukkit.getMobGoals().removeAllGoals(mob)}. MyPet's own
     * movement goals (follow owner, etc.) go through the Goal/Pathfinder
     * system and don't touch {@code WALK_TARGET}, so this clear leaves
     * MyPet-controlled movement intact while neutralizing brain-driven
     * roaming.
     *
     * <p>The rider's mount-controlled steering is also unaffected: vanilla
     * mount-control writes velocity directly to the entity rather than
     * routing through brain memory or the navigator.
     *
     * <p>Fail-soft: if the underlying reflection lookup ever breaks this
     * becomes a no-op. The pet's brain-driven roaming returns, which is a
     * UX regression but not a safety issue.
     */
    public static void clearWalkTarget(LivingEntity entity) {
        if (!initialized) tryInit(entity);
        if (!available) return;
        try {
            Object handle = getHandleMethod.invoke(entity);
            Object brain = getBrainMethod.invoke(handle);
            eraseMemoryMethod.invoke(brain, walkTargetMemoryType);
        } catch (Throwable t) {
            // Don't spam logs — drop silently after a successful init.
        }
    }

    /**
     * Strips every vanilla {@code BehaviorControl} from the entity's brain
     * whose runtime class's {@link Class#getSimpleName} matches one of the
     * supplied names. Called once at spawn from
     * {@link de.Keyle.MyPet.entity.spawn.PetGoalInstaller} after the
     * {@code removeAllGoals} sweep, driven by the per-pet
     * {@link de.Keyle.MyPet.api.brain.PetBrainBehaviorRemoval} declarations.
     *
     * <p>Walks the brain's {@code availableBehaviorsByPriority} schedule:
     * {@code Map<Integer, Map<Activity, Set<BehaviorControl>>>}. For every
     * behavior set at every priority and activity, removes the entries
     * whose simple class name matches, then recurses into any remaining
     * {@code GateBehavior} composite (the parent class of {@code RunOne},
     * {@code RunSometimes}, etc.) and strips matches from its inner
     * {@code ShufflingList} of child behaviors. Vanilla nests behaviors
     * inside {@code RunOne} routinely (e.g., {@code CamelAi$RandomSitting}
     * sits inside a {@code RunOne} alongside {@code RandomStroll}), so
     * without the recursion only top-level registrations would match.
     *
     * <p>Activities and priorities are left intact — only the specific
     * behaviors are extracted, so the brain's structure (running
     * activities, schedule slots) keeps working for any behaviors we
     * don't strip. A {@code RunOne} that ends up with fewer children
     * after a strip simply picks from the remaining options; if it ends
     * up empty its {@code tryStart} naturally fails.
     *
     * <p>Simple-name matching is deliberate: Mojang occasionally moves
     * classes between packages (e.g.,
     * {@code net.minecraft.world.entity.ai.behavior.*} →
     * {@code .behavior.copper_golem.*}) but rarely renames them. Per-pet
     * declarations stay readable ({@code "TransportItemsBetweenContainers"})
     * without tracking package paths.
     *
     * <p>Empty input is a no-op (no reflection performed). If the brain's
     * behavior-schedule field or the {@code GateBehavior} composite
     * reflection couldn't be located at init (Mojang rename), the
     * affected layer no-ops silently — the startup warnings told the
     * operator what's going on. Per-pet damage/movement guards aren't
     * affected.
     *
     * <p>Safe to call on goal-only mobs: their brain is empty so the
     * inner iteration is a no-op.
     */
    public static void removeBehaviorsByClassName(LivingEntity entity, Set<String> simpleClassNames) {
        if (simpleClassNames.isEmpty()) return;
        if (!initialized) tryInit(entity);
        if (!available) return;
        if (availableBehaviorsByPriorityField == null) return;
        try {
            Object handle = getHandleMethod.invoke(entity);
            Object brain = getBrainMethod.invoke(handle);
            Object schedule = availableBehaviorsByPriorityField.get(brain);
            if (!(schedule instanceof Map<?, ?> priorityMap)) return;
            for (Object activityMap : priorityMap.values()) {
                if (!(activityMap instanceof Map<?, ?> behaviorsByActivity)) continue;
                for (Object behaviorSet : behaviorsByActivity.values()) {
                    if (!(behaviorSet instanceof Set<?> set)) continue;
                    set.removeIf(behavior -> behavior != null
                            && simpleClassNames.contains(behavior.getClass().getSimpleName()));
                    for (Object remaining : set) {
                        stripFromComposite(remaining, simpleClassNames);
                    }
                }
            }
        } catch (Throwable t) {
            // Don't spam logs — drop silently after a successful init.
        }
    }

    /**
     * Recursively removes behaviors matching {@code simpleClassNames} from
     * a {@code GateBehavior} composite's inner {@code ShufflingList}. No-op
     * if {@code behavior} isn't a {@code GateBehavior} or if composite
     * reflection isn't available.
     */
    private static void stripFromComposite(Object behavior, Set<String> simpleClassNames) {
        if (behavior == null) return;
        if (gateBehaviorClass == null || !gateBehaviorClass.isInstance(behavior)) return;
        try {
            Object shufflingList = gateBehaviorBehaviorsField.get(behavior);
            if (shufflingList == null) return;
            Object entries = shufflingListEntriesField.get(shufflingList);
            if (!(entries instanceof List<?> list)) return;
            list.removeIf(entry -> {
                try {
                    Object data = weightedEntryGetDataMethod.invoke(entry);
                    return data != null && simpleClassNames.contains(data.getClass().getSimpleName());
                } catch (Throwable t) {
                    return false;
                }
            });
            for (Object entry : list) {
                try {
                    Object data = weightedEntryGetDataMethod.invoke(entry);
                    stripFromComposite(data, simpleClassNames);
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable t) {
            // Don't spam logs — drop silently after a successful init.
        }
    }
}

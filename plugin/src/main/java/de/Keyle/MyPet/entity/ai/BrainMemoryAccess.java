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

/**
 * Reflective bridge to vanilla's brain-memory system
 * ({@code net.minecraft.world.entity.ai.Brain} + {@code MemoryModuleType}).
 *
 * <p>Bukkit's {@link org.bukkit.entity.memory.MemoryKey} catalogue intentionally
 * excludes the combat-related {@code ATTACK_TARGET} memory (and several others
 * used by brain-driven AI). Brain-using mobs like Breeze, Warden, Piglin, and
 * Villager retain their full vanilla brain after MyPet's
 * {@link de.Keyle.MyPet.entity.spawn.PetGoalInstaller} runs
 * {@code Bukkit.getMobGoals().removeAllGoals(mob)} — the goal sweep doesn't
 * touch brains — so they keep populating {@code ATTACK_TARGET} from their
 * nearest-target sensors and launching autonomous attacks (wind charges,
 * sonic booms, etc.) at whoever happens to be nearby, including the owner.
 *
 * <p>This helper exposes the minimum surface needed to clear that memory
 * each tick from a {@link de.Keyle.MyPet.api.lifecycle.PetLifecycleHook}.
 * Same surgical-NMS-reflection pattern as
 * {@link de.Keyle.MyPet.entity.ai.movement.CubeMobMoveControlAccess} — a
 * deliberate exception to the codebase's "no NMS" stance for cases where
 * Bukkit doesn't expose the needed API. Mojang-mapped names are used
 * directly because Paper 1.20.5+ exposes the server jar under mojmap at
 * runtime.
 *
 * <p><b>Fail-soft:</b> if any lookup breaks (rename in a future MC version,
 * class loader oddity, etc.), a single warning is logged and subsequent
 * calls become no-ops. The brain-driven attacks return; the projectile-launch
 * suppressors on each affected pet class still neutralize the actual damage.
 */
public final class BrainMemoryAccess {

    private BrainMemoryAccess() {}

    private static volatile boolean initialized = false;
    private static volatile boolean available = false;
    private static volatile Method getHandleMethod;
    private static volatile Method getBrainMethod;
    private static volatile Method eraseMemoryMethod;
    private static volatile Object attackTargetMemoryType;

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

            available = true;
        } catch (Throwable t) {
            MyPetApi.getLogger().warning(
                    "BrainMemoryAccess unavailable — brain-driven autonomous attacks will not be suppressed at the brain level (projectile-launch suppressors still block actual damage). Cause: " + t);
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
}

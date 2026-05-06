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

package de.Keyle.MyPet.entity.visual;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.Pet;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Wither;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Clears the three head targets of a Wither pet every tick so the mob's
 * hardcoded combat logic in {@code WitherBoss#customServerAiStep} never finds
 * a target to fire at.
 *
 * <p>The Wither's side-head ({@link Wither.Head#LEFT}, {@link Wither.Head#RIGHT})
 * targeting is independent of the goal selector — it scans nearby entities
 * directly every ~15 ticks and writes an entity id into the mob's data watcher.
 * {@link de.Keyle.MyPet.entity.spawn.PetGoalInstaller} removes vanilla goals but
 * cannot suppress that scan, so the wither keeps picking victims and
 * {@code performRangedAttack()} keeps playing {@code SoundEvents.WITHER_SHOOT}
 * (via {@code levelEvent(1024, ...)}) before firing the skull. The skull itself
 * is already caught by {@code EntityListener#onPetAutonomousWitherSkull}, but the
 * sound plays client-side before the {@code ProjectileLaunchEvent} fires, so
 * suppressing the skull alone doesn't silence the attack.
 *
 * <p>Clearing all three head targets each tick removes the precondition for
 * {@code performRangedAttack()}: with no target on any head, the fire loop
 * never runs. The {@link Wither.Head#CENTER} target is also cleared — Pet
 * goals never set it (they update {@code Pet.targetEntity} internally, not
 * {@code Mob.setTarget}), but belt-and-suspenders guards against a stray
 * setter from an integration or a future refactor. The clear is gated on a
 * non-null read so we don't fire {@code EntityTargetEvent} 20 times per
 * second for no reason.
 *
 * <p>Per-pet scheduling follows {@link de.Keyle.MyPet.entity.ride.RideSkillFlightController}:
 * {@code mob.getScheduler().runAtFixedRate(...)} runs on the entity's region
 * thread on Folia and on the main thread on Paper, and is cancelled on despawn.
 */
public final class WitherAutonomousAttackSuppressor {

    private static final Map<UUID, ScheduledTask> tasks = new ConcurrentHashMap<>();

    private WitherAutonomousAttackSuppressor() {
    }

    public static void startForPet(Pet pet) {
        Mob mob = pet.getBukkitEntity();
        if (!(mob instanceof Wither wither)) return;

        Plugin plugin = MyPetApi.getPlugin();
        UUID key = pet.getUUID();
        stopForPet(pet);

        ScheduledTask task = mob.getScheduler().runAtFixedRate(plugin, t -> {
            try {
                if (wither.isDead()) return;
                clearIfSet(wither, Wither.Head.CENTER);
                clearIfSet(wither, Wither.Head.LEFT);
                clearIfSet(wither, Wither.Head.RIGHT);
            } catch (Throwable ignored) {
            }
        }, null, 1L, 1L);
        if (task != null) {
            tasks.put(key, task);
        }
    }

    public static void stopForPet(Pet pet) {
        ScheduledTask task = tasks.remove(pet.getUUID());
        if (task != null) {
            try {
                task.cancel();
            } catch (Exception ignored) {
            }
        }
    }

    private static void clearIfSet(Wither wither, Wither.Head head) {
        if (wither.getTarget(head) != null) {
            wither.setTarget(head, null);
        }
    }
}

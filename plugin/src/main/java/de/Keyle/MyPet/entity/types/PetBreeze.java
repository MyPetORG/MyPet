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

package de.Keyle.MyPet.entity.types;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.config.ConfigKey;
import de.Keyle.MyPet.api.entity.DefaultInfo;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.PetFlyingEntity;
import de.Keyle.MyPet.api.entity.ShopInfo;
import de.Keyle.MyPet.api.lifecycle.PetLifecycleHook;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.entity.PetImpl;
import de.Keyle.MyPet.entity.ai.BrainAccess;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Material;
import org.bukkit.entity.Breeze;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ShopInfo
@DefaultInfo(food = {Material.GUNPOWDER}, flySpeed = 1.3877D)
public class PetBreeze extends PetImpl implements PetFlyingEntity {

    public static final ConfigKey<Boolean> CAN_FLY = ConfigKey.bool("Breeze", "CanFly", true);

    /**
     * Vanilla brain AI disabled for this pet, admin-overridable in pet-config.yml.
     * Empty by default — MyPet strips nothing from this species' brain. The key
     * exists so an admin can disable brain AI here without a plugin change;
     * entries are {@code activity:<name>} or {@code behavior:<SimpleClassName>}.
     */
    public static final ConfigKey<List<String>> BRAIN_DISABLED =
            ConfigKey.stringList("Breeze", "Brain.Disabled");

    public static final PetLifecycleHook LIFECYCLE_HOOK = new PetLifecycleHook(
            "Breeze",
            AutonomousAttackSuppressor::startForPet,
            AutonomousAttackSuppressor::stopForPet
    );

    public PetBreeze(MyPetPlayer petOwner) {
        super(petOwner);
    }

    /**
     * Erases the Breeze pet's brain {@code ATTACK_TARGET} memory every tick so
     * the vanilla {@code Shoot} behavior (under the {@code ATTACK} activity)
     * has no target to wind up against.
     *
     * <p>Breeze AI is entirely brain-based — its attack pipeline lives in
     * {@code Behavior<Breeze>} classes that read {@code ATTACK_TARGET} from
     * brain memory, not in any {@code Goal}.
     * {@link de.Keyle.MyPet.entity.spawn.PetGoalInstaller}'s
     * {@code Bukkit.getMobGoals().removeAllGoals(mob)} sweep strips goals but
     * leaves the brain intact, so without this suppressor the brain's
     * nearest-target sensor keeps populating {@code ATTACK_TARGET} (often
     * with the owner) and the {@code Shoot} behavior keeps playing the
     * windup animation and sound before firing a wind charge.
     *
     * <p>Wiping the memory each tick interrupts the {@code Shoot} behavior's
     * {@code canStillUse()} check on the next tick, ending the windup
     * early (the activity selector also won't re-enter {@code ATTACK} on
     * subsequent ticks because the memory is gone). A 1-tick window of
     * windup visual may still flash before the clear lands — acceptable
     * trade-off and identical in shape to the Wither AutonomousAttackSuppressor
     * note. No projectile-launch-side fallback is needed because the
     * {@code Shoot} behavior's ~50-tick windup is orders of magnitude
     * longer than our 1-tick clear interval — there is no intra-tick race
     * that can let a wind charge slip through (unlike Wither's same-tick
     * scan-and-fire path, which is why the Wither has a launch-side
     * backup and Breeze does not).
     *
     * <p>If {@link BrainAccess} ever becomes unavailable (NMS rename
     * in a future MC version, etc.), the helper logs a single startup
     * warning and {@code clearAttackTarget} becomes a no-op. Wind charges
     * would then fire normally — same failure mode as
     * {@link de.Keyle.MyPet.entity.ai.movement.CubeMobMoveControlAccess}
     * losing slime body rotation, and addressed the same way: fix the
     * reflection lookup.
     *
     * <p>Per-pet scheduling follows {@link de.Keyle.MyPet.entity.ride.RideSkillFlightController}:
     * {@code mob.getScheduler().runAtFixedRate(...)} runs on the entity's
     * region thread on Folia and on the main thread on Paper, and is
     * cancelled on despawn.
     */
    public static final class AutonomousAttackSuppressor {

        private static final Map<UUID, ScheduledTask> tasks = new ConcurrentHashMap<>();

        private AutonomousAttackSuppressor() {
        }

        public static void startForPet(Pet pet) {
            Mob mob = pet.getBukkitEntity();
            if (!(mob instanceof Breeze breeze)) return;

            Plugin plugin = MyPetApi.getPlugin();
            UUID key = pet.getUUID();
            stopForPet(pet);

            ScheduledTask task = mob.getScheduler().runAtFixedRate(plugin, t -> {
                try {
                    if (breeze.isDead()) return;
                    BrainAccess.clearAttackTarget(breeze);
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
    }
}

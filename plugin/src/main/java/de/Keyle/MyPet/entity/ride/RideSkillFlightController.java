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

package de.Keyle.MyPet.entity.ride;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.skill.UpgradeComputer;
import de.Keyle.MyPet.api.skill.skills.Ride;
import de.Keyle.MyPet.entity.PetClimbSupport;
import de.Keyle.MyPet.entity.PetAttributes;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Input;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Drives a pet's velocity from its rider's WASD/jump/sneak input when the Ride
 * skill is active.
 *
 * <p>Per-pet scheduling: one task per pet with an active Ride skill, started on
 * spawn (or when Ride activates later) and cancelled on despawn. Fuel state is
 * held in the controller instance so each pet gets its own fuel value naturally
 * (no more shared map with leaked entries).
 */
public class RideSkillFlightController {

    private static final Map<UUID, ScheduledTask> tasks = new ConcurrentHashMap<>();
    private static final Map<UUID, RideSkillFlightController> controllers = new ConcurrentHashMap<>();
    /** Watchers on the Ride skill's active state for pets spawned without it. */
    private static final Map<UUID, UpgradeComputer.UpgradeCallback<Boolean>> activationWatchers = new ConcurrentHashMap<>();

    /**
     * Air friction in vanilla's leaky-integrator travel pipeline
     * ({@code LivingEntity#travel} for in-air motion). Per tick vanilla does
     * {@code v += input × flyingSpeed; v ×= friction;}, so steady-state velocity
     * is {@code flyingSpeed / (1 - friction)}.
     */
    private static final double VANILLA_AIR_FRICTION = 0.91;

    /**
     * Effective ground friction in vanilla's walk-travel integrator.
     * Vanilla {@code LivingEntity#travel} on the ground multiplies the block's
     * static friction (default {@code 0.6} for stone/dirt/etc.) by the same
     * {@code 0.91} scalar used in-air, giving an effective ground friction
     * of {@code 0.546}. Terminal walking velocity in vanilla is therefore
     * {@code movementSpeed / (1 - 0.546)}.
     */
    private static final double VANILLA_GROUND_FRICTION = 0.6 * 0.91;

    /**
     * Multiplier that maps a vanilla {@code FLYING_SPEED} attribute value
     * (a per-tick force coefficient applied through the air integrator) to
     * the direct-per-tick velocity that would equal vanilla's steady-state
     * terminal velocity for that input. Derived as
     * {@code 1 / (1 - VANILLA_AIR_FRICTION) ≈ 11.11}.
     *
     * <p>Applied at the attribute read site in {@link #resolveBaseSpeed} so
     * that third-party plugins setting vanilla {@code FLYING_SPEED}
     * (baseValue or modifiers) drive ride speed without needing a MyPet API
     * dependency — they tune the vanilla number; we translate units on read.
     */
    private static final double FLYING_SPEED_TO_DIRECT_VELOCITY = 1.0 / (1.0 - VANILLA_AIR_FRICTION);

    /**
     * Multiplier that maps a vanilla {@code MOVEMENT_SPEED} attribute value
     * (a per-tick force coefficient applied through the ground integrator)
     * to the direct-per-tick velocity equivalent. Derived as
     * {@code 1 / (1 - VANILLA_GROUND_FRICTION) ≈ 2.20}.
     *
     * <p>Distinct from {@link #FLYING_SPEED_TO_DIRECT_VELOCITY} because
     * vanilla applies different friction in the on-ground vs in-air paths —
     * using the air factor on walking-speed values turns a typical Pig's
     * {@code 0.25} into {@code 2.78 b/t (~55 m/s)} of ridden motion, which
     * was the regression that prompted splitting these constants.
     */
    private static final double MOVEMENT_SPEED_TO_DIRECT_VELOCITY = 1.0 / (1.0 - VANILLA_GROUND_FRICTION);

    /** Fuel remaining (ticks) for this pet. */
    private double fuelTicks = -1;

    public static void startForPet(Pet pet) {
        Mob mob = pet.getBukkitEntity();
        if (mob == null) return;
        stopForPet(pet);
        Ride rideSkill = rideSkill(pet);
        UpgradeComputer<Boolean> active = rideSkill != null ? rideSkill.getActive() : null;
        if (active == null) return;
        if (!Boolean.TRUE.equals(active.getValue())) {
            // No Ride skill yet — skip the per-tick task. Watch the skill's
            // active state so pets that gain Ride later (skilltree apply,
            // level-up) still get their task.
            UpgradeComputer.UpgradeCallback<Boolean> watcher = (value, reason) -> {
                // Idempotent: a skilltree re-apply (e.g. on level-up) re-notifies
                // with an unchanged TRUE value; rescheduling would cancel and
                // rebuild the running task, resetting ride fuel. Start only when
                // no task is running yet.
                if (Boolean.TRUE.equals(value) && !tasks.containsKey(pet.getUUID())) {
                    scheduleTask(pet);
                }
            };
            activationWatchers.put(pet.getUUID(), watcher);
            active.addCallback(watcher);
            return;
        }
        scheduleTask(pet);
    }

    private static void scheduleTask(Pet pet) {
        Mob mob = pet.getBukkitEntity();
        if (mob == null) return;
        Plugin plugin = MyPetApi.getPlugin();
        UUID key = pet.getUUID();
        cancelTask(key);
        RideSkillFlightController controller = new RideSkillFlightController();
        controllers.put(key, controller);
        ScheduledTask task = mob.getScheduler().runAtFixedRate(plugin, t -> {
            try {
                controller.tickPet(pet);
            } catch (Throwable ignored) {
            }
        }, null, 1L, 1L);
        if (task != null) {
            tasks.put(key, task);
        }
    }

    public static void stopForPet(Pet pet) {
        UUID key = pet.getUUID();
        cancelTask(key);
        controllers.remove(key);
        UpgradeComputer.UpgradeCallback<Boolean> watcher = activationWatchers.remove(key);
        if (watcher != null) {
            Ride rideSkill = rideSkill(pet);
            if (rideSkill != null && rideSkill.getActive() != null) {
                rideSkill.getActive().removeCallback(watcher);
            }
        }
    }

    private static void cancelTask(UUID key) {
        ScheduledTask task = tasks.remove(key);
        if (task != null) {
            try {
                task.cancel();
            } catch (Exception ignored) {
            }
        }
    }

    private static Ride rideSkill(Pet pet) {
        try {
            return pet.getSkills().get(Ride.class);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Resolves the base per-tick ride speed for a pet/mob pair using this
     * resolution chain:
     * <ol>
     *   <li>If {@link de.Keyle.MyPet.api.entity.PetInfo#isOverrideFlySpeed}
     *       is {@code true} for this pet type, return
     *       {@link de.Keyle.MyPet.api.entity.PetInfo#getFlySpeed} verbatim
     *       (direct-per-tick-velocity units, no scaling);</li>
     *   <li>Otherwise, live {@code FLYING_SPEED} attribute on the mob
     *       (naturally-flying species) multiplied by
     *       {@link #FLYING_SPEED_TO_DIRECT_VELOCITY} to convert from
     *       vanilla's air-integrator force-coefficient units;</li>
     *   <li>Otherwise, live {@code MOVEMENT_SPEED} attribute (ground/aquatic
     *       pets that gained flight via the Ride skilltree's {@code CanFly}
     *       upgrade) multiplied by {@link #MOVEMENT_SPEED_TO_DIRECT_VELOCITY}
     *       to convert from vanilla's ground-integrator force-coefficient
     *       units (a different friction constant than the air path, hence a
     *       different multiplier);</li>
     *   <li>Final fallback {@code 0.6} for pets with neither attribute
     *       registered.</li>
     * </ol>
     *
     * <p>Called from {@link #tickPet} and from {@code PetEnderDragon.HoverController.tickRide}
     * so both controllers stay in sync on the resolution policy.
     */
    public static double resolveBaseSpeed(Pet pet, Mob mob) {
        if (MyPetApi.getPetInfo().isOverrideFlySpeed(pet.getPetType())) {
            return MyPetApi.getPetInfo().getFlySpeed(pet.getPetType());
        }
        AttributeInstance flyAttr = mob.getAttribute(PetAttributes.FLYING_SPEED);
        if (flyAttr != null) {
            return flyAttr.getValue() * FLYING_SPEED_TO_DIRECT_VELOCITY;
        }
        AttributeInstance walkAttr = mob.getAttribute(PetAttributes.MOVEMENT_SPEED);
        if (walkAttr != null) {
            return walkAttr.getValue() * MOVEMENT_SPEED_TO_DIRECT_VELOCITY;
        }
        return 0.6;
    }

    private void tickPet(Pet pet) {
        Mob mob = pet.getBukkitEntity();
        if (mob == null || mob.isDead()) return;
        if (mob.isEmpty()) return;
        // EnderDragon's vanilla aiStep applies ~0.8 horizontal friction to
        // setVelocity calls every tick, so velocity-based control feels
        // unresponsive. PetEnderDragon.HoverController owns dragon ride
        // movement via teleport, which overrides aiStep cleanly.
        if (mob instanceof EnderDragon) return;

        List<Entity> passengers = mob.getPassengers();
        if (passengers.isEmpty() || !(passengers.get(0) instanceof Player rider)) return;

        if (pet.getOwner() == null || pet.getOwner().getPlayer() == null
                || !rider.getUniqueId().equals(pet.getOwner().getPlayer().getUniqueId())) {
            return;
        }

        Ride rideSkill;
        try {
            rideSkill = pet.getSkills().get(Ride.class);
        } catch (Throwable t) {
            return;
        }
        if (rideSkill == null) return;
        if (rideSkill.getActive() == null || rideSkill.getActive().getValue() == null
                || !rideSkill.getActive().getValue()) {
            return;
        }

        Input input;
        try {
            input = rider.getCurrentInput();
        } catch (Throwable t) {
            return;
        }
        if (input == null) return;

        float yaw = rider.getLocation().getYaw();
        float pitch = rider.getLocation().getPitch();
        mob.setRotation(yaw, 0);

        double baseSpeed = resolveBaseSpeed(pet, mob);
        int speedIncrease = rideSkill.getSpeedIncrease() != null && rideSkill.getSpeedIncrease().getValue() != null
                ? rideSkill.getSpeedIncrease().getValue() : 0;
        double speed = baseSpeed * (1.0 + speedIncrease / 100.0);

        double fx = 0;
        double fz = 0;
        if (input.isForward()) fx += 1;
        if (input.isBackward()) fx -= 0.5;
        if (input.isLeft()) fz -= 0.5;
        if (input.isRight()) fz += 0.5;

        double radYaw = Math.toRadians(yaw);
        double radPitch = Math.toRadians(pitch);
        double cosPitch = Math.cos(radPitch);
        // Forward (W/S) follows the rider's look vector — looking down tilts the
        // motion downward, looking up tilts it upward. Strafe (A/D) stays purely
        // horizontal so sideways input doesn't drift vertically.
        double worldX = (-Math.sin(radYaw) * fx * cosPitch - Math.cos(radYaw) * fz) * speed;
        double worldZ = (Math.cos(radYaw) * fx * cosPitch - Math.sin(radYaw) * fz) * speed;
        double pitchInducedY = -Math.sin(radPitch) * fx * speed;

        boolean canFly = rideSkill.getCanFly() != null && Boolean.TRUE.equals(rideSkill.getCanFly().getValue());
        double flyLimitSeconds = rideSkill.getFlyLimit() != null && rideSkill.getFlyLimit().getValue() != null
                ? rideSkill.getFlyLimit().getValue().doubleValue() : 0;
        double flyRegen = rideSkill.getFlyRegenRate() != null && rideSkill.getFlyRegenRate().getValue() != null
                ? rideSkill.getFlyRegenRate().getValue().doubleValue() : 0;

        if (fuelTicks < 0) {
            fuelTicks = flyLimitSeconds * 20.0;
        }

        double worldY = mob.getVelocity().getY();
        // Tracks whether worldY was actually written by rider intent this
        // tick (jump-driven lift, or pitch-driven climb/dive). When false,
        // worldY is just the pre-existing gravity-affected velocity — vanilla
        // is driving vertical motion, not the rider, so fallDistance should
        // continue to accumulate normally.
        boolean riderDroveY = false;

        boolean unlimitedFuel = flyLimitSeconds <= 0;
        if (input.isJump()) {
            if (mob.isOnGround() || mob.isInWater() || mob.isInLava()) {
                double jumpHeight = rideSkill.getJumpHeight() != null && rideSkill.getJumpHeight().getValue() != null
                        ? rideSkill.getJumpHeight().getValue().doubleValue() : 0.5;
                worldY = Math.max(0.42, jumpHeight * 0.2);
                riderDroveY = true;
            } else if (canFly && (unlimitedFuel || fuelTicks > 0)) {
                worldY = 0.35;
                riderDroveY = true;
                if (!unlimitedFuel) {
                    fuelTicks = Math.max(0, fuelTicks - 1);
                }
            }
        } else {
            // Pitch-driven vertical motion: only kicks in while the rider is
            // pressing forward/backward, so a stationary look-around doesn't
            // bob the pet up and down. Gated on canFly - without flight,
            // looking up while pressing W would otherwise lift the pet off
            // the ground. Looking up while falling would halt the fall
            // (the pitchInducedY write replaces the gravity-derived velocity
            // wholesale, so even clamping to non-positive would still suspend
            // a falling pet at 0 Y velocity mid-air).
            if (fx != 0 && canFly) {
                worldY = pitchInducedY;
                riderDroveY = true;
            }
            if (flyLimitSeconds > 0) {
                double maxFuel = flyLimitSeconds * 20.0;
                double regenPerTick = flyRegen > 0 ? (flyRegen / 20.0) : 0.5;
                fuelTicks = Math.min(maxFuel, fuelTicks + regenPerTick);
            }
        }

        // Climb skill: spider-style wall climbing while ridden. Only when the
        // rider is not already driving Y (jump/flight win), the pet has no
        // flight (flying pets don't need to climb), the rider holds forward,
        // and the pet is actually pressing against a wall in its movement
        // direction. Water/lava are excluded — jump-driven swimming handles
        // vertical motion there.
        if (!riderDroveY && !canFly && !pet.getPetType().isFlyingPet()
                && input.isForward() && !mob.isInWater() && !mob.isInLava()
                && isClimbActive(pet) && PetClimbSupport.isWallAhead(mob, worldX, worldZ)) {
            worldY = PetClimbSupport.CLIMB_SPEED;
            riderDroveY = true;
        }

        mob.setVelocity(new Vector(worldX, worldY, worldZ));
        // Reset fallDistance only when the rider actually drove vertical
        // motion this tick. Vanilla's accumulator would otherwise treat a
        // rider-controlled descent as a free-fall and apply fall damage on
        // landing equal to the full drop. When the rider stops driving Y
        // (no jump, no W/S, or W/S without canFly), worldY is just the
        // pre-existing gravity velocity — let vanilla accumulate so a pet
        // the rider abandons mid-air still takes legitimate fall damage.
        if (riderDroveY) {
            mob.setFallDistance(0f);
        }
    }

    /** Returns whether the ridden pet has the Ride skill's climb upgrade unlocked. */
    private static boolean isClimbActive(Pet pet) {
        Ride rideSkill;
        try {
            rideSkill = pet.getSkills().get(Ride.class);
        } catch (Throwable t) {
            return false;
        }
        return rideSkill != null && rideSkill.getClimb() != null
                && Boolean.TRUE.equals(rideSkill.getClimb().getValue());
    }
}

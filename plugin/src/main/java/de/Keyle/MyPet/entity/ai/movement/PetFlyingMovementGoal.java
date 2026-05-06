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

package de.Keyle.MyPet.entity.ai.movement;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import de.Keyle.MyPet.api.entity.Pet;
import org.bukkit.entity.Mob;
import de.Keyle.MyPet.entity.PetAttributes;
import de.Keyle.MyPet.entity.ai.PetGoalKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * Per-tick velocity and rotation driver for flying pets.
 *
 * <p>Runs every tick and writes flying movement physics directly via the
 * Paper API ({@link Mob#setVelocity}, {@link org.bukkit.entity.Entity#setRotation})
 * — the goal reads the current pathfinder state to determine movement
 * direction, clamps the per-tick step to the remaining distance, and
 * interpolates the pet's yaw and pitch toward the velocity vector.
 *
 * <p><b>Pre-v4 note:</b> in the NMS-era this goal was paired with an
 * {@code EntityMyFlyingPet} override that suppressed
 * {@code getMoveControl().tick()} so the vanilla ground-based
 * {@code MoveControl} wouldn't fight this goal's direct velocity writes.
 * That entity layer is gone; the equivalent suppression now happens via
 * the goal-strip in {@code PetGoalInstaller}.
 */
public class PetFlyingMovementGoal implements Goal<Mob> {

    private static final double RAD_TO_DEG = 57.2957763671875D;
    private static final double EPSILON_TOTAL_SQ = 2.500000277905201E-7D;
    private static final double EPSILON_DIRECTION = 9.999999747378752E-6D;

    /**
     * Scale factor applied to the {@code MOVEMENT_SPEED} attribute when
     * computing the per-tick direct velocity for a flying pet.
     *
     * <p>The {@code MOVEMENT_SPEED} attribute on flying pets is tuned by
     * {@link PetFollowOwnerGoal} and the various speed-boost paths for an
     * <em>accumulated-force</em> movement pipeline — the attribute acts as
     * a scalar applied to an existing delta-movement vector, with the
     * entity's own {@code travel()} supplying the actual push. For a pet
     * near its owner the attribute typically sits around 0.87, and under
     * "far chase" or sprint boosts it can climb past 1.0.
     *
     * <p>This goal instead writes velocity <em>directly</em> each tick via
     * {@link Mob#setVelocity}. Using the attribute value unscaled produces
     * velocities of ~17 blocks/sec or higher, which overshoots the target
     * every tick and causes the pet to ping-pong around its owner. This
     * factor brings the per-tick direct velocity into the 4–9 blocks/sec
     * range at typical attribute values — comparable to a sprinting player,
     * which matches the intended feel.
     */
    private static final double DIRECT_VELOCITY_SCALE = 0.25D;

    private final Pet pet;
    private final Mob mob;
    private final float maxTurn;

    // Movement target state
    private boolean hasTarget = false;
    private double wantedX, wantedY, wantedZ;
    private double speedModifier = 1.0D;

    public PetFlyingMovementGoal(Pet pet, Mob mob, float maxTurn) {
        this.pet = pet;
        this.mob = mob;
        this.maxTurn = maxTurn;
    }

    /**
     * Sets the movement target. Called from the {@code MoveControl} bridge
     * installed in {@code EntityMyFlyingPet} when the path navigation issues
     * a move-to request; {@code speedModifier} is the multiplicative scalar
     * that the pathfinder attaches to each waypoint.
     */
    public void setWantedPosition(double x, double y, double z, double speedModifier) {
        this.wantedX = x;
        this.wantedY = y;
        this.wantedZ = z;
        this.speedModifier = speedModifier;
        this.hasTarget = true;
    }

    public void clearTarget() {
        this.hasTarget = false;
    }

    @Override
    public boolean shouldActivate() {
        // Always active: this goal drives movement execution every tick.
        return true;
    }

    @Override
    public boolean shouldStayActive() {
        return true;
    }

    @Override
    public void tick() {
        if (!Bukkit.isOwnedByCurrentRegion(mob)) {
            return;
        }
        if (hasTarget) {
            hasTarget = false; // Consume the target — one velocity write per setWantedPosition call

            Location loc = mob.getLocation();

            double dx = wantedX - loc.getX();
            double dy = wantedY - loc.getY();
            double dz = wantedZ - loc.getZ();
            double totalDistSq = dx * dx + dy * dy + dz * dz;

            if (totalDistSq < EPSILON_TOTAL_SQ) {
                // Already at the target — no motion needed.
                return;
            }

            double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            double totalDist = Math.sqrt(totalDistSq);

            // Rotate toward target. Skipped when horizontally aligned so yaw
            // doesn't snap from an undefined atan2(0, 0) result — but the
            // velocity block below still runs so vertical motion happens.
            if (horizontalDist > EPSILON_DIRECTION) {
                float targetYaw = (float) (Math.atan2(dz, dx) * RAD_TO_DEG) - 90.0F;
                float targetPitch = (float) (-(Math.atan2(-dy, horizontalDist) * RAD_TO_DEG));

                float newYaw = rotlerp(loc.getYaw(), targetYaw, 90.0F);
                float newPitch = rotlerp(loc.getPitch(), targetPitch, maxTurn);
                mob.setRotation(newYaw, newPitch);
                mob.setBodyYaw(newYaw);
            }

            // Compute per-tick velocity. See DIRECT_VELOCITY_SCALE javadoc for why
            // the MOVEMENT_SPEED attribute is scaled down — direct velocity writing
            // requires a much smaller magnitude than the accumulated-force pipeline
            // the attribute was tuned for.
            double baseSpeed = mob.getAttribute(PetAttributes.MOVEMENT_SPEED).getValue();
            double perTickSpeed = baseSpeed * speedModifier * DIRECT_VELOCITY_SCALE;

            // Clamp to the remaining distance so a single tick cannot overshoot
            // the target. This is the hard stop that prevents ping-pong around
            // a stationary owner, independent of how the scale factor is tuned.
            if (perTickSpeed > totalDist) {
                perTickSpeed = totalDist;
            }

            double invDist = 1.0D / totalDist;
            mob.setVelocity(new Vector(
                    dx * invDist * perTickSpeed,
                    dy * invDist * perTickSpeed,
                    dz * invDist * perTickSpeed));
        }
        // When no target: let friction in EntityMyFlyingPet.travel() decelerate
        // the entity naturally.
    }

    /**
     * Smoothly interpolates {@code current} toward {@code target}, stepping
     * at most {@code maxDelta} degrees per call.
     */
    private static float rotlerp(float current, float target, float maxDelta) {
        float delta = wrapDegrees(target - current);
        if (delta > maxDelta) delta = maxDelta;
        if (delta < -maxDelta) delta = -maxDelta;
        return current + delta;
    }

    /**
     * Wraps a degree value to the {@code [-180, 180]} range.
     */
    private static float wrapDegrees(float degrees) {
        degrees = degrees % 360.0F;
        if (degrees >= 180.0F) degrees -= 360.0F;
        if (degrees < -180.0F) degrees += 360.0F;
        return degrees;
    }

    @Override
    public @NotNull GoalKey<Mob> getKey() {
        return PetGoalKey.FLYING_MOVEMENT;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        // UNKNOWN_BEHAVIOR: This goal does not conflict with MOVE goals (like FollowOwner)
        // because it operates at the movement-execution layer, not the movement-decision layer.
        // Using MOVE would prevent FollowOwner and other MOVE goals from running.
        return EnumSet.of(GoalType.UNKNOWN_BEHAVIOR);
    }
}

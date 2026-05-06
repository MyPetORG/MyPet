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
import de.Keyle.MyPet.entity.ai.PetGoalKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * Owner-following goal for cube mobs (currently {@link Slime} and {@link org.bukkit.entity.MagmaCube}).
 *
 * <p>Vanilla {@link Slime} and {@link org.bukkit.entity.MagmaCube} use a special
 * {@code SlimeMoveControl} that drives their bouncy hop motion via its own jump-cycle
 * and rotation system — and crucially, it does not consume paths from the standard
 * {@code Pathfinder} / Bukkit navigation. So the standard {@link PetFollowOwnerGoal}'s
 * {@code nav.navigateTo(owner)} call is silently ignored by cube mobs; they stay in place
 * until they fall behind enough for the teleport-snap to trip.
 *
 * <p>This goal replaces the navigation loop with direct hop-driving via
 * {@link Mob#setVelocity(Vector)}: each tick, when the cube mob is on the ground and the
 * cadence timer says "go", apply a size-aware jump impulse aimed at the owner.
 *
 * <p>Cadence is distance-graded: ~30 ticks between hops when within {@link #CLOSE_RANGE}
 * (calm idle look), interpolating down to ~10 ticks at {@link #CRUISE_RANGE} (aggressive
 * catch-up). The numeric thresholds intentionally mirror {@link PetFollowOwnerGoal}'s
 * range constants but are kept independent — the semantics differ (this goal uses them
 * for discrete cadence, the parent goal uses them for continuous speed multipliers).
 *
 * <p>Cross-region snap, cross-world bail, and distance-triggered teleport behaviors are
 * delegated to {@link PetFollowOwnerSupport} so they stay byte-identical with
 * {@link PetFollowOwnerGoal}.
 */
public class PetCubeMobFollowOwnerGoal implements Goal<Mob> {

    // -------------------------------------------------------------------------
    // DISTANCE THRESHOLDS - Mirror PetFollowOwnerGoal's CLOSE_RANGE / CRUISE_RANGE
    // numerically but kept independent because they drive cadence here, not speed.
    // -------------------------------------------------------------------------

    /** Below this distance the cadence is at its slowest (calm hop). */
    private static final double CLOSE_RANGE = 4.0;

    /** At/above this distance the cadence is at its fastest (catch-up hop). */
    private static final double CRUISE_RANGE = 10.0;

    /**
     * Squared distance below which the cube mob stops trying to hop closer — matches
     * {@link PetFollowOwnerGoal#FULL_STOP_DIST_SQ} (~1.4 blocks). Without this, the
     * cube mob visibly oscillates inside the deadzone trying to move toward an owner
     * already standing on top of it.
     */
    private static final double FULL_STOP_DIST_SQ = 2.0;

    /**
     * Squared distance constraint when the owner is stationary — matches
     * {@link PetFollowOwnerGoal#STATIONARY_MAX_DIST_SQ} so the dead zone with
     * {@code PetRandomStrollGoal} is consistent across pet types.
     */
    private static final double STATIONARY_MAX_DIST_SQ = 9.0;

    /**
     * Speed below which the owner is considered stationary — matches
     * {@link PetFollowOwnerGoal#OWNER_STATIONARY_THRESHOLD}.
     */
    private static final double OWNER_STATIONARY_THRESHOLD = 0.03;

    // -------------------------------------------------------------------------
    // HOP CADENCE - Ticks between successive hops, distance-graded
    // -------------------------------------------------------------------------

    /** Slowest hop cadence (ticks between hops) — applies when within {@link #CLOSE_RANGE}. */
    private static final int CADENCE_CALM = 30;

    /** Fastest hop cadence (ticks between hops) — applies at/beyond {@link #CRUISE_RANGE}. */
    private static final int CADENCE_AGGRESSIVE = 10;

    // -------------------------------------------------------------------------
    // HOP STRENGTH - Size-aware jump impulse
    // -------------------------------------------------------------------------

    /**
     * Base upward velocity for a size-1 slime hop. Matches vanilla
     * {@code Slime#getJumpPower()} of {@code 0.42 + 0.10 * (size - 1)}.
     */
    private static final double JUMP_BASE = 0.42;

    /** Per-size-step contribution to upward jump velocity — matches vanilla. */
    private static final double JUMP_PER_SIZE = 0.10;

    /**
     * Base horizontal speed per hop. Combined with {@link #FORWARD_PER_SIZE} via
     * {@code FORWARD_BASE + FORWARD_PER_SIZE * size}, so a size-1 slime hops at 0.24
     * blocks/tick horizontally and a size-4 slime at 0.36. Approximates vanilla horizontal
     * travel-per-hop, which vanilla derives implicitly from {@code xxa = 1.0} integrated
     * against size-scaled friction over airborne ticks. Tunable post-merge if it feels
     * wrong.
     */
    private static final double FORWARD_BASE = 0.20;

    /** Per-size-step contribution to horizontal speed. */
    private static final double FORWARD_PER_SIZE = 0.04;

    // -------------------------------------------------------------------------
    // LOOK-AT-OWNER - Cadence for head-tracking, mirrors parent's LOOKAT_INTERVAL
    // -------------------------------------------------------------------------

    private static final int LOOKAT_INTERVAL = 5;

    /** Tick warm-up before the goal begins acting — matches parent goal's settling window. */
    private static final int WARMUP_TICKS = 5;

    // ==================== INSTANCE FIELDS ====================

    private final Pet pet;
    private final Mob mob;
    private final Slime slime;
    private final double startDistance;
    private final double stopDistance;
    private final double teleportDistance;

    private PetControlGoal controlPathfinderGoal;
    /** Sentinel for the controlPathfinderGoal lookup — see {@link PetFollowOwnerGoal} for rationale. */
    private boolean controlGoalLookupDone = false;

    private final PetFollowOwnerSupport.TeleportState teleportState =
            new PetFollowOwnerSupport.TeleportState();

    /** Refreshed every tick — see {@link PetFollowOwnerGoal} for rationale. */
    private Player owner;

    private int lookAtTimer = 0;
    private int tickCounter = 0;
    private int hopCooldown = 0;

    // Owner-speed tracking — used only for the stationary-owner range tightening.
    private double lastOwnerX = 0;
    private double lastOwnerZ = 0;
    private double ownerMovementSpeed = 0;
    private int lastTrackingTick = -1;

    public PetCubeMobFollowOwnerGoal(Pet pet, Mob mob,
                                     double startDistance, float stopDistance, float teleportDistance) {
        this.pet = pet;
        this.mob = mob;
        if (!(mob instanceof Slime s)) {
            throw new IllegalArgumentException(
                    "PetCubeMobFollowOwnerGoal requires a cube mob (Slime or MagmaCube); got " + mob.getClass().getName());
        }
        this.slime = s;
        this.startDistance = startDistance * startDistance;
        this.stopDistance = stopDistance * stopDistance;
        this.teleportDistance = teleportDistance * teleportDistance;
    }

    /** Refreshes {@link #owner} from the {@link Pet}; returns true if the owner is online. */
    private boolean refreshOwner() {
        if (pet.getOwner() == null) {
            this.owner = null;
            return false;
        }
        this.owner = pet.getOwner().getPlayer();
        return this.owner != null;
    }

    @Override
    public boolean shouldActivate() {
        if (!Bukkit.isOwnedByCurrentRegion(mob)) {
            return false;
        }
        if (controlPathfinderGoal == null && !controlGoalLookupDone) {
            Goal<Mob> goal = Bukkit.getMobGoals().getGoal(pet.getBukkitEntity(), PetGoalKey.CONTROL);
            if (goal instanceof PetControlGoal pcg) {
                controlPathfinderGoal = pcg;
            }
            controlGoalLookupDone = true;
        }
        if (!this.pet.canMove()) {
            return false;
        }
        if (this.pet.getPetTarget() != null && !this.pet.getPetTarget().isDead()) {
            return false;
        }
        if (!refreshOwner()) {
            return false;
        }
        double distSqToOwner = mob.getLocation().distanceSquared(owner.getLocation());
        double effectiveStartDistance = this.startDistance;
        if (ownerMovementSpeed < OWNER_STATIONARY_THRESHOLD) {
            effectiveStartDistance = STATIONARY_MAX_DIST_SQ;
        }
        if (distSqToOwner < effectiveStartDistance) {
            return false;
        }
        return controlPathfinderGoal == null || controlPathfinderGoal.moveTo == null;
    }

    @Override
    public boolean shouldStayActive() {
        if (!Bukkit.isOwnedByCurrentRegion(mob)) {
            return false;
        }
        if (controlPathfinderGoal != null && controlPathfinderGoal.moveTo != null) {
            return false;
        }
        if (!refreshOwner()) {
            return false;
        }
        double effectiveStopDistance = this.stopDistance;
        if (ownerMovementSpeed < OWNER_STATIONARY_THRESHOLD) {
            effectiveStopDistance = STATIONARY_MAX_DIST_SQ;
        }
        double distSq = mob.getLocation().distanceSquared(owner.getLocation());
        if (distSq < effectiveStopDistance) {
            return false;
        }
        if (!this.pet.canMove()) {
            return false;
        }
        if (this.pet.getPetTarget() != null && !this.pet.getPetTarget().isDead()) {
            return false;
        }
        return true;
    }

    @Override
    public void start() {
        if (!Bukkit.isOwnedByCurrentRegion(mob)) {
            return;
        }
        refreshOwner();
        hopCooldown = 0;
    }

    @Override
    public void stop() {
        ownerMovementSpeed = 0;
        lastTrackingTick = -1;
        teleportState.waitForGround = false;
        hopCooldown = 0;
        tickCounter = 0;
    }

    @Override
    public void tick() {
        if (!Bukkit.isOwnedByCurrentRegion(mob)) {
            return;
        }
        tickCounter++;
        if (tickCounter <= WARMUP_TICKS) {
            return;
        }
        if (!refreshOwner()) {
            return;
        }
        if (PetFollowOwnerSupport.isCrossWorld(mob, owner)) {
            return;
        }
        if (PetFollowOwnerSupport.snapAcrossRegionsIfNeeded(pet, mob, owner, null)) {
            return;
        }

        // Owner movement tracking — needed only for the stationary-owner range tightening.
        Location ownerLoc = owner.getLocation();
        double currentOwnerX = ownerLoc.getX();
        double currentOwnerZ = ownerLoc.getZ();
        int currentTick = Bukkit.getCurrentTick();
        int ticksSinceLastUpdate = currentTick - lastTrackingTick;
        lastTrackingTick = currentTick;
        if (ticksSinceLastUpdate <= 2 && ticksSinceLastUpdate > 0) {
            double dx = currentOwnerX - lastOwnerX;
            double dz = currentOwnerZ - lastOwnerZ;
            double currentMovement = Math.sqrt(dx * dx + dz * dz);
            if (ticksSinceLastUpdate > 1) {
                currentMovement /= ticksSinceLastUpdate;
            }
            // Same EMA weights as PetFollowOwnerGoal for consistency.
            ownerMovementSpeed = 0.2 * currentMovement + 0.8 * ownerMovementSpeed;
        }
        lastOwnerX = currentOwnerX;
        lastOwnerZ = currentOwnerZ;

        Location petLoc = mob.getLocation();
        double distanceSqr = petLoc.distanceSquared(ownerLoc);
        double distance = Math.sqrt(distanceSqr);

        if (--lookAtTimer <= 0) {
            mob.lookAt(owner, mob.getHeadRotationSpeed(), mob.getMaxHeadPitch());
            lookAtTimer = LOOKAT_INTERVAL;
        }

        boolean controlIsMoving = controlPathfinderGoal != null && controlPathfinderGoal.moveTo != null;
        if (PetFollowOwnerSupport.teleportIfTooFar(pet, mob, owner, distanceSqr, teleportDistance,
                false, controlIsMoving, null, teleportState)) {
            return;
        }

        // Inside the deadzone — don't try to move closer.
        if (distanceSqr < FULL_STOP_DIST_SQ) {
            return;
        }

        double dx = ownerLoc.getX() - petLoc.getX();
        double dz = ownerLoc.getZ() - petLoc.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        if (horizontalDist < 1.0e-3) {
            return;
        }
        double dirX = dx / horizontalDist;
        double dirZ = dz / horizontalDist;

        // Face owner every tick. Vanilla SlimeMoveControl.tick() rotlerps yaw toward its
        // internal yRot field by max 90° per tick — without writing that field via reflection
        // the slime body stays locked at its spawn rotation regardless of what setRotation()
        // does (vanilla MoveControl runs after our goal and overwrites the result).
        // CubeMobMoveControlAccess.setDirection writes the field so vanilla rotates toward the
        // owner. setRotation/setBodyYaw still run for first-frame visual snap.
        float yaw = (float) (Math.toDegrees(Math.atan2(-dx, dz)));
        mob.setRotation(yaw, 0f);
        mob.setBodyYaw(yaw);
        CubeMobMoveControlAccess.setDirection(slime, yaw);

        // Hop driver — distance-graded cadence + size-aware impulse.
        if (hopCooldown > 0) {
            hopCooldown--;
            return;
        }
        if (!mob.isOnGround()) {
            return;
        }

        int size = slime.getSize();
        double jumpY = JUMP_BASE + JUMP_PER_SIZE * (size - 1);
        double forwardSpeed = FORWARD_BASE + FORWARD_PER_SIZE * size;

        mob.setVelocity(new Vector(dirX * forwardSpeed, jumpY, dirZ * forwardSpeed));

        hopCooldown = computeCadence(distance);
    }

    /**
     * Linear interpolation between {@link #CADENCE_CALM} (at/below {@link #CLOSE_RANGE})
     * and {@link #CADENCE_AGGRESSIVE} (at/above {@link #CRUISE_RANGE}).
     */
    private static int computeCadence(double distance) {
        if (distance <= CLOSE_RANGE) {
            return CADENCE_CALM;
        }
        if (distance >= CRUISE_RANGE) {
            return CADENCE_AGGRESSIVE;
        }
        double t = (distance - CLOSE_RANGE) / (CRUISE_RANGE - CLOSE_RANGE);
        return (int) Math.round(CADENCE_CALM + t * (CADENCE_AGGRESSIVE - CADENCE_CALM));
    }

    @Override
    public @NotNull GoalKey<Mob> getKey() {
        return PetGoalKey.FOLLOW_OWNER_CUBE_MOB;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE);
    }
}

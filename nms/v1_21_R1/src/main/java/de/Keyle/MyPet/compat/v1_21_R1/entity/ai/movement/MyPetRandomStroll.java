/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2025 Keyle
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

package de.Keyle.MyPet.compat.v1_21_R1.entity.ai.movement;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.ai.AIGoal;
import de.Keyle.MyPet.api.entity.ai.navigation.AbstractNavigation;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_21_R1.entity.EntityMyPet;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftPlayer;

/**
 * AI goal that makes ground-based Pets wander randomly near their stationary owner.
 *
 * <p>This goal activates only when the owner is stationary (not moving) and the Pet
 * is within a close range. It creates natural idle behavior where Pets meander
 * around their owner instead of standing still.
 *
 * <p>Key behaviors:
 * <ul>
 *   <li>Only activates when owner movement speed is below threshold (0.03 blocks/tick)</li>
 *   <li>Constrains wandering to within 3 blocks of owner when stationary</li>
 *   <li>Uses slow, relaxed movement speed independent of player's walk speed</li>
 *   <li>Attempts up to 5 random positions to find a valid wander target</li>
 * </ul>
 *
 * <p>This is the base class for {@link MyPetRandomFly} and {@link MyPetRandomSwim}
 * which provide specialized wandering for flying and aquatic Pets.
 *
 * @see MyPetRandomFly
 * @see MyPetRandomSwim
 * @see FollowOwner
 */
@Compat("v1_21_R1")
public class MyPetRandomStroll implements AIGoal, de.Keyle.MyPet.api.entity.ai.movement.MyPetRandomStroll {

    // ==================== TUNABLE CONSTANTS ====================
    // These values control random stroll behavior and can be adjusted to fine-tune idle wandering.
    // All distance values are in blocks, speeds are in blocks/tick unless noted.

    // -------------------------------------------------------------------------
    // DISTANCE THRESHOLDS - Define boundaries for stroll activation
    // -------------------------------------------------------------------------

    /**
     * Maximum squared distance (in blocks²) from owner for stroll activation.
     * Pet must be within sqrt(9) = 3 blocks of a stationary owner to start strolling.
     * Must match {@link FollowOwner#STATIONARY_MAX_DIST_SQ} to prevent dead zones.
     */
    protected static final double STATIONARY_MAX_DIST_SQ = 9.0;

    // -------------------------------------------------------------------------
    // STROLL ACTIVATION - Parameters controlling when strolling starts
    // -------------------------------------------------------------------------

    /**
     * Base probability (0.0-1.0) of starting a stroll each tick when conditions are met.
     * Value of 0.02 means 2% chance per tick, approximately one stroll attempt every 2.5 seconds.
     * Subclasses override: flying Pets use 8%, swimming Pets use 6%.
     */
    protected static final float DEFAULT_STROLL_CHANCE = 0.02F;

    // -------------------------------------------------------------------------
    // OWNER MOVEMENT DETECTION - Parameters for stationary detection
    // -------------------------------------------------------------------------

    /**
     * Speed threshold (blocks/tick) below which owner is considered stationary.
     * Normal walking is ~0.1 blocks/tick. Value of 0.03 detects when owner is
     * standing still or barely moving (e.g., minor position adjustments).
     */
    private static final double OWNER_STATIONARY_THRESHOLD = 0.03;

    /**
     * Weight for current frame's speed in exponential moving average (EMA).
     * Formula: newSpeed = RESPONSIVE * current + (1 - RESPONSIVE) * previous
     * Higher values = more responsive but jittery detection.
     */
    private static final double SPEED_SMOOTHING_RESPONSIVE = 0.2;

    /**
     * Maximum ticks between position updates for valid movement calculation.
     * If more than 2 ticks pass between updates (e.g., goal was inactive),
     * movement calculation is skipped to avoid false speed readings.
     */
    private static final int MAX_TRACKING_GAP = 2;

    // -------------------------------------------------------------------------
    // WANDER POSITION GENERATION - Parameters for target selection
    // -------------------------------------------------------------------------

    /**
     * Horizontal wander radius (blocks) when owner is stationary.
     * Pet will look for random positions within this radius.
     * Smaller value keeps Pet close to stationary owner.
     */
    protected static final int WANDER_RADIUS_STATIONARY = 2;

    /**
     * Horizontal wander radius (blocks) when owner is moving.
     * Larger radius allows more freedom but rarely used since
     * strolling typically only activates when owner is stationary.
     */
    protected static final int WANDER_RADIUS_MOVING = 3;

    /**
     * Maximum squared distance (blocks²) from owner for wander targets when stationary.
     * Targets beyond this distance from owner are rejected.
     */
    protected static final double WANDER_MAX_DIST_SQ_STATIONARY = STATIONARY_MAX_DIST_SQ;

    /**
     * Maximum squared distance (blocks²) from Pet's current position for wander targets when stationary.
     * Value of 4.0 = 2 blocks. Prevents Pet from wandering too far in a single stroll.
     */
    protected static final double WANDER_MAX_DIST_SQ_FROM_PET_STATIONARY = 4.0;

    /**
     * Maximum squared distance (blocks²) from Pet's current position when owner is moving.
     * Value of 9.0 = 3 blocks. Allows slightly longer strolls.
     */
    protected static final double WANDER_MAX_DIST_SQ_FROM_PET_MOVING = 9.0;

    /**
     * Maximum attempts to find a valid wander position before giving up.
     * Higher values increase chance of finding valid position but cost more CPU.
     */
    protected static final int WANDER_MAX_ATTEMPTS = 5;

    // -------------------------------------------------------------------------
    // MOVEMENT SPEED - Parameters for stroll movement
    // -------------------------------------------------------------------------

    /**
     * Movement speed (blocks/tick) for ground Pet strolling.
     * Slow, relaxed pace independent of owner's configured walk speed.
     * Creates natural idle wandering rather than darting movement.
     */
    protected static final double STROLL_SPEED = 0.15;

    /**
     * Minimum time (ticks) for a stroll to complete.
     * Prevents very short strolls that look unnatural.
     */
    protected static final int MIN_STROLL_TIME = 3;

    /**
     * Distance divisor for calculating stroll timeout.
     * timeout = max(MIN_STROLL_TIME, distance / TIMEOUT_DIVISOR)
     * Value of 3 gives approximately 1 tick per 3 blocks of distance.
     */
    protected static final int STROLL_TIMEOUT_DIVISOR = 3;

    /**
     * Distance threshold (blocks) for considering stroll destination reached.
     * When Pet is closer than this to target, stroll completes successfully.
     */
    protected static final double DESTINATION_REACHED_THRESHOLD = 0.75;

    // ==================== INSTANCE FIELDS ====================

    /** The Pet entity performing the stroll behavior. */
    protected final EntityMyPet petEntity;

    /** Navigation system for pathfinding. */
    protected AbstractNavigation nav;

    /** Current destination for the stroll, or null if no active stroll. */
    protected Location moveTo = null;

    /** Remaining ticks before the current stroll times out. */
    protected int timeToMove = 0;

    /** Maximum squared distance from owner before strolling is allowed. */
    protected final int startDistance;

    /** Reference to the Control goal for checking manual Pet control. */
    protected Control controlPathfinderGoal;

    /** The Pet's owner as an NMS Player entity. */
    protected final Player owner;

    /** Probability (0.0-1.0) of starting a stroll each tick. */
    protected float strollChance = DEFAULT_STROLL_CHANCE;

    /** Whether the owner is currently stationary (moving less than threshold). */
    protected boolean ownerStationary = false;

    /** Last tracked X position of owner for movement calculation. */
    private double lastOwnerX = 0;

    /** Last tracked Z position of owner for movement calculation. */
    private double lastOwnerZ = 0;

    /** Smoothed owner movement speed in blocks per tick. */
    protected double ownerMovementSpeed = 0;

    /** Last tick when movement tracking was updated. */
    private int lastTrackingTick = -1;

    /**
     * Updates owner movement tracking using position-based delta calculation.
     *
     * <p>This method must be called each tick for accurate readings. It uses position
     * tracking instead of {@code getDeltaMovement()} because the latter is unreliable
     * for player entities (often returns near-zero even when walking).
     *
     * <p>The movement speed is smoothed using exponential moving average (20% current, 80% previous)
     * to prevent jitter from affecting the stationary detection.
     */
    protected void updateOwnerMovementTracking() {
        double currentOwnerX = owner.getX();
        double currentOwnerZ = owner.getZ();
        int currentTick = petEntity.tickCount;
        int ticksSinceLastUpdate = currentTick - lastTrackingTick;
        lastTrackingTick = currentTick;

        // Only calculate movement if we have continuous tracking (no gaps)
        if (ticksSinceLastUpdate <= MAX_TRACKING_GAP && ticksSinceLastUpdate > 0) {
            double dx = currentOwnerX - lastOwnerX;
            double dz = currentOwnerZ - lastOwnerZ;
            double currentMovement = Math.sqrt(dx * dx + dz * dz);
            if (ticksSinceLastUpdate > 1) {
                currentMovement /= ticksSinceLastUpdate;
            }
            ownerMovementSpeed = SPEED_SMOOTHING_RESPONSIVE * currentMovement + (1.0 - SPEED_SMOOTHING_RESPONSIVE) * ownerMovementSpeed;
        }
        lastOwnerX = currentOwnerX;
        lastOwnerZ = currentOwnerZ;

        // Owner is stationary if actual movement is very small
        ownerStationary = ownerMovementSpeed < OWNER_STATIONARY_THRESHOLD;
    }

    /**
     * Creates a new random stroll AI goal for the specified Pet.
     *
     * @param entityMyPet   the Pet entity that will perform random strolls
     * @param startDistance maximum distance from owner for strolling (will be squared internally)
     */
    public MyPetRandomStroll(EntityMyPet entityMyPet, int startDistance) {
        this.petEntity = entityMyPet;
        this.nav = entityMyPet.getPetNavigation();
        this.startDistance = startDistance * startDistance;
        this.owner = ((CraftPlayer) petEntity.getOwner().getPlayer()).getHandle();
    }

    @Override
    public boolean shouldStart() {
        if (this.petEntity.getRandom().nextFloat() >= this.strollChance) {
            return false;
        }

        if (controlPathfinderGoal == null) {
            if (petEntity.getPathfinder().hasGoal("Control")) {
                controlPathfinderGoal = (Control) petEntity.getPathfinder().getGoal("Control");
            }
        }
        if (!this.petEntity.canMove()) {
            return false;
        } else if (this.petEntity.getMyPetTarget() != null && !this.petEntity.getMyPetTarget().isDead()) {
            return false;
        } else if (this.petEntity.getOwner() == null) {
            return false;
        }

        // Check if owner is stationary using position-based tracking
        // (getDeltaMovement is unreliable for players - returns near-zero even when walking)
        updateOwnerMovementTracking();

        // Only allow strolling when owner is truly stationary
        // If owner is moving, Pet should be following, not strolling
        if (!ownerStationary) {
            return false;
        }

        double distSqToOwner = this.petEntity.distanceToSqr(owner);

        // When owner is stationary, use tight constraint
        if (distSqToOwner <= STATIONARY_MAX_DIST_SQ) {
            // Only stroll if close enough to owner and not being controlled
            return controlPathfinderGoal == null || controlPathfinderGoal.moveTo == null;
        } else return false;
    }

    @Override
    public boolean shouldFinish() {
        if (controlPathfinderGoal != null && controlPathfinderGoal.moveTo != null) {
            return true;
        } else if (this.petEntity.getOwner() == null) {
            return true;
        }

        // Re-check if owner is stationary using position-based tracking
        updateOwnerMovementTracking();

        // Use tighter constraint when owner is stationary
        double maxDistSq = ownerStationary ? STATIONARY_MAX_DIST_SQ : this.startDistance;
        double distSqToOwner = this.petEntity.distanceToSqr(owner);

        if (distSqToOwner > maxDistSq) {
            return true;
        } else if (!this.petEntity.canMove()) {
            return true;
        } else if (moveTo == null) {
            return true;
        } else if (petEntity.getMyPet().getLocation()
                .map(loc -> MyPetApi.getPlatformHelper().distance(loc, moveTo) < DESTINATION_REACHED_THRESHOLD)
                .orElse(true)) {
            return true;
        } else if (timeToMove <= 0) {
            return true;
        } else if (this.petEntity.getMyPetTarget() != null && !this.petEntity.getMyPetTarget().isDead()) {
            return true;
        }
        return false;
    }

    @Override
    public void start() {
        Vec3 vec = getPosition();
        if(vec == null) {
            return;
        }
        applySpeed();
        moveTo = new Location(this.petEntity.getBukkitEntity().getWorld(), vec.x, vec.y, vec.z);
        timeToMove = petEntity.getMyPet().getLocation()
                .map(loc -> (int) MyPetApi.getPlatformHelper().distance(loc, moveTo) / STROLL_TIMEOUT_DIVISOR)
                .orElse(MIN_STROLL_TIME);
        timeToMove = Math.max(timeToMove, MIN_STROLL_TIME);

        if(!nav.navigateTo(moveTo)) {
            this.moveTo = null;
        }
    }

    @Override
    public void finish() {
        nav.getParameters().removeSpeedModifier("RandomStroll");
        this.nav.stop();
    }

    /**
     * Generates a random position for the Pet to wander to.
     *
     * <p>The position is constrained to be within range of both the owner and the Pet's
     * current position. When the owner is stationary, tighter constraints are used
     * (2-block radius vs 3-block radius).
     *
     * <p>The method attempts up to 5 random positions using {@link LandRandomPos} before
     * giving up and returning null.
     *
     * @return a valid wander target position, or {@code null} if no suitable position found
     */
    protected Vec3 getPosition() {
        // When owner is stationary, use a smaller wander radius
        int wanderRadius = ownerStationary ? WANDER_RADIUS_STATIONARY : WANDER_RADIUS_MOVING;
        double maxDistSqFromOwner = ownerStationary ? WANDER_MAX_DIST_SQ_STATIONARY : this.startDistance;
        // Also limit how far the Pet travels from its current position
        double maxDistSqFromPet = ownerStationary ? WANDER_MAX_DIST_SQ_FROM_PET_STATIONARY : WANDER_MAX_DIST_SQ_FROM_PET_MOVING;

        Vec3 petPos = petEntity.position();

        // Get a random position, but ensure it stays within range of both owner and Pet
        for (int i = 0; i < WANDER_MAX_ATTEMPTS; i++) {
            Vec3 vec = LandRandomPos.getPos(this.petEntity, wanderRadius, 0);
            if (vec != null) {
                // Check distance from target to owner
                double distSqToOwner = vec.distanceToSqr(owner.getX(), owner.getY(), owner.getZ());
                // Check distance from target to Pet's current position
                double distSqToPet = vec.distanceToSqr(petPos);

                if (distSqToOwner < maxDistSqFromOwner && distSqToPet < maxDistSqFromPet) {
                    return vec;
                }
            }
        }
        // If no valid position found, stay near current position
        return null;
    }

    /**
     * Applies the movement speed for strolling.
     *
     * <p>Uses a slow, constant speed (0.15) independent of the player's configured walk speed.
     * This prevents Pets from darting around erratically when the player has high walk speed
     * but is standing still.
     *
     * <p>Subclasses may override this to provide different speeds for flying or swimming Pets.
     */
    protected void applySpeed() {
        nav.getParameters().addSpeedModifier("RandomStroll", STROLL_SPEED);
    }

    /**
     * Called each tick while the stroll is active to decrement the timeout counter.
     */
    public void schedule() {
        timeToMove--;
    }
}

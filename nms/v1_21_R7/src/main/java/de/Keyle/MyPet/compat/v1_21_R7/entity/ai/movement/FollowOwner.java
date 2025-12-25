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

package de.Keyle.MyPet.compat.v1_21_R7.entity.ai.movement;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.ai.AIGoal;
import de.Keyle.MyPet.api.entity.ai.navigation.AbstractNavigation;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_21_R7.entity.EntityMyFlyingPet;
import de.Keyle.MyPet.compat.v1_21_R7.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_21_R7.entity.ai.navigation.MyAquaticPetPathNavigation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftPlayer;

/**
 * AI goal that makes a Pet follow its owner with intelligent speed matching and movement control.
 *
 * <p>This goal implements an owner-following behavior that adapts to different scenarios:
 * <ul>
 *   <li><b>Cruise Mode</b> (distance &lt; 10 blocks): Speed matches owner's movement for natural following</li>
 *   <li><b>FAR Mode</b> (distance &gt;= 10 blocks): Rapid catch-up with boosted speed</li>
 *   <li><b>Teleportation</b>: Automatic teleport when Pet falls too far behind</li>
 * </ul>
 *
 * <p>The goal handles three types of Pets differently:
 * <ul>
 *   <li><b>Ground Pets</b>: Use pathfinder-based movement with direct MOVEMENT_SPEED attribute control</li>
 *   <li><b>Flying Pets</b>: Use velocity-based movement with hover height maintenance above owner</li>
 *   <li><b>Swimming Pets</b>: Use velocity-based movement with water resistance compensation</li>
 * </ul>
 *
 * <p>Key features include:
 * <ul>
 *   <li>Owner speed tracking via position delta (not unreliable getDeltaMovement)</li>
 *   <li>Velocity steering to prevent overshooting</li>
 *   <li>Progressive braking when approaching owner</li>
 *   <li>Surface swimming for non-aquatic Pets when owner is in water</li>
 * </ul>
 *
 * @see MyPetRandomStroll
 * @see Control
 */
@Compat("v1_21_R7")
public class FollowOwner implements AIGoal {

	// ==================== TUNABLE CONSTANTS ====================
	// These values control owner-following behavior and can be adjusted to fine-tune movement.
	// All distance values are in blocks, speeds are in blocks/tick unless noted.

	// -------------------------------------------------------------------------
	// DISTANCE THRESHOLDS - Define the boundaries between movement modes
	// -------------------------------------------------------------------------

	/**
	 * Maximum distance (in blocks) for CRUISE mode activation.
	 * Below this distance, Pet speed scales with owner's movement speed.
	 * Above this distance, FAR mode activates with boosted catch-up speed.
	 */
	private static final double CRUISE_RANGE = 10.0;

	/**
	 * Distance threshold (in blocks) for "close" speed settings.
	 * When Pet is closer than this to owner, it uses slower multipliers
	 * to avoid overshooting and oscillating around the owner.
	 */
	private static final double CLOSE_RANGE = 4.0;

	/**
	 * Distance threshold (in blocks) where speed transitions from close to catching-up.
	 * Between CLOSE_RANGE and TRANSITION_RANGE, speed multipliers interpolate.
	 * Above TRANSITION_RANGE (but below CRUISE_RANGE), full catch-up multipliers apply.
	 */
	private static final double TRANSITION_RANGE = 5.0;

	/**
	 * Squared distance threshold for full stop behavior.
	 * When Pet is closer than sqrt(2.0) ≈ 1.4 blocks, navigation stops
	 * and velocity is zeroed to prevent jittering around the owner.
	 */
	private static final double FULL_STOP_DIST_SQ = 2.0;

	/**
	 * Horizontal distance (in blocks) at which surface swimming stops.
	 * When a non-aquatic Pet is surface-swimming toward an owner in water,
	 * it will stop and float once within this horizontal distance.
	 */
	private static final double SURFACE_SWIM_CLOSE = 3.0;

	// -------------------------------------------------------------------------
	// OWNER MOVEMENT TRACKING - Parameters for detecting owner speed
	// -------------------------------------------------------------------------

	/**
	 * Speed threshold (blocks/tick) below which owner is considered stationary.
	 * Normal walking is ~0.1 blocks/tick, sprinting ~0.13, speed 2 walking ~0.2.
	 * Value of 0.03 detects when owner is standing still or barely moving.
	 */
	private static final double OWNER_STATIONARY_THRESHOLD = 0.03;

	/**
	 * Weight for current tick's speed in exponential moving average (EMA).
	 * Higher values make speed tracking more responsive but jittery.
	 * Formula: newSpeed = RESPONSIVE * current + STABLE * previous
	 */
	private static final double SPEED_SMOOTHING_RESPONSIVE = 0.2;

	/**
	 * Weight for previous speed in exponential moving average (EMA).
	 * Higher values make speed tracking more stable but slower to react.
	 * Must equal (1.0 - SPEED_SMOOTHING_RESPONSIVE) for proper EMA.
	 */
	private static final double SPEED_SMOOTHING_STABLE = 0.8;

	/**
	 * Decay rate per tick for the owner speed peak tracker.
	 * Peak tracking prevents Pets from slowing down prematurely when owner
	 * briefly pauses. Value of 0.95 means peak decays ~5% per tick.
	 */
	private static final double SPEED_PEAK_DECAY = 0.95;

	// -------------------------------------------------------------------------
	// SPEED MULTIPLIER CAPS - Prevent extreme speeds at high player speeds
	// -------------------------------------------------------------------------

	/**
	 * Maximum speed multiplier cap for Pets.
	 */
	private static final double MAX_SPEED_MULTIPLIER = 2.5;

	// -------------------------------------------------------------------------
	// FLYING/SWIMMING CRUISE MULTIPLIERS - Speed scaling for velocity-based Pets
	// -------------------------------------------------------------------------

	/**
	 * Speed multiplier for flying/swimming Pets when close to owner (< CLOSE_RANGE).
	 * Applied to owner's movement speed to calculate Pet cruise speed.
	 * Higher than ground Pets because velocity caps limit actual movement.
	 */
	private static final double FLY_SWIM_CRUISE_CLOSE_MULT = 1.5;

	/**
	 * Speed multiplier for flying/swimming Pets when catching up (> TRANSITION_RANGE).
	 * Maximum multiplier used in cruise mode for these Pet types.
	 */
	private static final double FLY_SWIM_CRUISE_FAR_MULT = 2.5;

	/**
	 * Distance factor for flying/swimming Pets beyond TRANSITION_RANGE.
	 * For each block beyond transition, distanceFactor increases by this amount.
	 * Example: At 8 blocks (3 beyond transition), factor = 1.0 + 3 * 0.15 = 1.45
	 */
	private static final double FLY_SWIM_CRUISE_DIST_FACTOR = 0.15;

	// -------------------------------------------------------------------------
	// GROUND PET CRUISE MULTIPLIERS - Speed scaling for pathfinder-based Pets
	// -------------------------------------------------------------------------

	/**
	 * Speed multiplier for ground Pets when close to owner (< CLOSE_RANGE).
	 * Lower than flying/swimming because ground pathfinding is more efficient
	 * and doesn't have velocity cap overhead.
	 */
	private static final double GROUND_CRUISE_CLOSE_MULT = 1.2;

	/**
	 * Speed multiplier for ground Pets when catching up (> TRANSITION_RANGE).
	 * Maximum multiplier used in cruise mode for ground Pets.
	 */
	private static final double GROUND_CRUISE_FAR_MULT = 1.8;

	/**
	 * Interpolation scale for ground Pet speed in transition zone.
	 * Between CLOSE_RANGE and TRANSITION_RANGE, multiplier interpolates:
	 * mult = CLOSE_MULT + (distance - CLOSE_RANGE) * TRANSITION_SCALE
	 */
	private static final double GROUND_CRUISE_TRANSITION_SCALE = 0.6;

	/**
	 * Distance factor for ground Pets beyond TRANSITION_RANGE.
	 * For each block beyond transition, distanceFactor increases by this amount.
	 * Lower than flying/swimming because ground pathfinding is more direct.
	 */
	private static final double GROUND_CRUISE_DIST_FACTOR = 0.1;

	// -------------------------------------------------------------------------
	// MINIMUM CRUISE SPEEDS - Floor values to prevent sluggish movement
	// -------------------------------------------------------------------------

	/**
	 * Minimum cruise speed for flying and swimming Pets.
	 * Ensures Pets keep moving even when owner is stationary or moving slowly.
	 * Higher than ground Pets due to 3D pathing overhead and medium resistance.
	 */
	private static final float FLY_SWIM_MIN_CRUISE = 0.5f;

	/**
	 * Minimum cruise speed for ground Pets.
	 * Ensures responsive movement even when owner moves slowly.
	 * Lower than flying/swimming because ground pathing is more efficient.
	 */
	private static final float GROUND_MIN_CRUISE = 0.25f;

	// -------------------------------------------------------------------------
	// FLYING PET SPECIFIC - Parameters for aerial Pet behavior
	// -------------------------------------------------------------------------

	/**
	 * Target hover height (in blocks) above owner's position.
	 * Flying Pets maintain this altitude offset to stay visible
	 * and avoid clipping into the owner's model.
	 */
	private static final double HOVER_HEIGHT = 1.5;

	/**
	 * Bonus speed added to flying Pets when beyond CLOSE_RANGE.
	 * Helps flying Pets keep up since they have more complex 3D pathing.
	 */
	private static final float FLYING_BONUS_SPEED = 0.1f;

	/**
	 * Maximum walk speed for flying Pets in FAR mode.
	 * Caps pathfinder-requested speed to prevent over-acceleration
	 * that would then be harshly corrected by velocity caps.
	 */
	private static final float FLYING_WALK_SPEED_CAP = 1.0f;

	// -------------------------------------------------------------------------
	// SWIMMING PET SPECIFIC - Parameters for aquatic Pet behavior
	// -------------------------------------------------------------------------

	/**
	 * Bonus speed added to swimming Pets to compensate for water resistance.
	 * Water significantly slows entity movement, so aquatic Pets need
	 * higher base speeds to keep pace with their owner.
	 */
	private static final float SWIMMING_BONUS_SPEED = 0.6f;

	/**
	 * Additional speed bonus when owner has Dolphin's Grace effect.
	 * Allows Pet to keep up when owner swims faster with the effect.
	 */
	private static final float DOLPHINS_GRACE_BONUS = 0.08f;

	/**
	 * Maximum speed for surface swimming (non-aquatic Pets following owner in water).
	 * Caps horizontal velocity to prevent unrealistic surface skating.
	 */
	private static final double SURFACE_SWIM_MAX_SPEED = 0.3;

	/**
	 * Base speed for surface swimming before distance scaling.
	 * Combined with distance factor: speed = min(MAX, BASE + distance * 0.01)
	 */
	private static final double SURFACE_SWIM_BASE_SPEED = 0.1;

	/**
	 * Velocity blending factor for surface swimming.
	 * Controls how much of current velocity vs target velocity is used.
	 * Value of 0.5 means 50% current + 50% target for smooth transitions.
	 */
	private static final double SURFACE_SWIM_BLEND = 0.5;

	// -------------------------------------------------------------------------
	// VELOCITY CAP MARGINS - Control maximum velocity relative to owner speed
	// -------------------------------------------------------------------------

	/**
	 * Velocity cap margin when Pet is close to owner (< CLOSE_RANGE).
	 * Pet's max velocity = ownerSpeed * margin. Value < 1.0 means Pet
	 * moves slower than owner to prevent catch-up oscillation.
	 */
	private static final double VEL_CAP_CLOSE_MARGIN = 0.75;

	/**
	 * Velocity cap margin for flying/swimming Pets when catching up.
	 * Value > 1.0 allows Pet to move faster than owner to close distance.
	 * Higher than ground Pets due to 3D path inefficiency.
	 */
	private static final double VEL_CAP_FAR_MARGIN_HIGH = 1.1;

	/**
	 * Velocity cap margin for ground Pets when catching up.
	 * Value < 1.0 keeps ground Pets from overshooting since their
	 * pathfinding is more direct and efficient than flying/swimming.
	 */
	private static final double VEL_CAP_FAR_MARGIN_LOW = 0.80;

	/**
	 * Minimum velocity cap for flying/swimming Pets (blocks/tick).
	 * Ensures Pets can still move when owner is stationary.
	 * Higher than ground Pets due to medium resistance overhead.
	 */
	private static final double VEL_CAP_MIN_HIGH = 0.25;

	/**
	 * Minimum velocity cap for ground Pets (blocks/tick).
	 * Ensures Pets can still move when owner is stationary.
	 */
	private static final double VEL_CAP_MIN_LOW = 0.15;

	// -------------------------------------------------------------------------
	// STEERING - Parameters for course correction toward owner
	// -------------------------------------------------------------------------

	/**
	 * Base alignment threshold for steering activation (cosine of angle).
	 * Value of 0.5 ≈ cos(60°), so steering activates when Pet is
	 * more than 60° off-course from direct path to owner.
	 */
	private static final double STEER_BASE_THRESHOLD = 0.5;

	/**
	 * Maximum alignment threshold at high speeds (cosine of angle).
	 * Value of 0.7 ≈ cos(45°). At higher speeds, steering activates
	 * earlier to prevent overshooting due to momentum.
	 */
	private static final double STEER_MAX_THRESHOLD = 0.7;

	/**
	 * Base steering strength factor.
	 * Determines how aggressively velocity is redirected toward owner.
	 * Higher values = sharper turns, lower values = smoother curves.
	 */
	private static final double STEER_BASE_FACTOR = 0.3;

	/**
	 * Maximum steering strength factor.
	 * Caps how aggressively the Pet can turn, preventing jarring
	 * instant direction changes. Applied when severely off-course.
	 */
	private static final double STEER_MAX_FACTOR = 0.85;

	/**
	 * Minimum horizontal speed (blocks/tick) required for steering.
	 * Below this speed, steering is skipped to avoid jitter when nearly stopped.
	 */
	private static final double STEER_MIN_SPEED = 0.1;

	/**
	 * Minimum distance to owner (blocks) required for steering.
	 * Below this distance, steering is skipped since direction matters less.
	 */
	private static final double STEER_MIN_DIST = 0.5;

	// -------------------------------------------------------------------------
	// BRAKING - Parameters for slowing down when approaching owner
	// -------------------------------------------------------------------------

	/**
	 * Minimum braking distance (blocks) for flying/swimming Pets.
	 * Within this distance, braking logic activates to prevent overshooting.
	 * Actual brake distance may be larger based on speed multiplier.
	 */
	private static final double BRAKE_MIN_DISTANCE = 3.0;

	/**
	 * Minimum speed (blocks/tick) before braking is applied.
	 * If Pet is already moving slowly, braking is skipped.
	 */
	private static final double BRAKE_SPEED_THRESHOLD = 0.25;

	/**
	 * Brake factor at distance = 0 (right at owner).
	 * Speed is multiplied by this factor, so 0.3 means reduce to 30% speed.
	 * Lower values = harder braking when very close.
	 */
	private static final double BRAKE_FACTOR_MIN = 0.3;

	/**
	 * Brake factor at brake distance boundary.
	 * Interpolates from MIN to MAX based on distance within brake zone.
	 * Higher values = gentler braking at edge of brake zone.
	 */
	private static final double BRAKE_FACTOR_MAX = 0.7;

	// -------------------------------------------------------------------------
	// FAR MODE - Parameters for catch-up when beyond CRUISE_RANGE
	// -------------------------------------------------------------------------

	/**
	 * Maximum speed boost in FAR mode (added to base walk speed).
	 * Applied when Pet is far behind owner and needs to catch up quickly.
	 */
	private static final float FAR_SPEED_BOOST_MAX = 0.07f;

	/**
	 * Speed boost scaling factor per block beyond TRANSITION_RANGE.
	 * boost = min(MAX, (distance - TRANSITION_RANGE) * SCALE)
	 * Provides gradual speed increase as Pet falls further behind.
	 */
	private static final double FAR_SPEED_BOOST_SCALE = 0.014;

	/**
	 * Maximum speed multiplier in FAR mode.
	 * Caps the effective multiplier to prevent unrealistic speeds
	 * even when owner has high /speed settings.
	 */
	private static final float FAR_MAX_MULTIPLIER = 1.5f;

	// -------------------------------------------------------------------------
	// BASE VALUES - Fundamental constants used in calculations
	// -------------------------------------------------------------------------

	/**
	 * Base speed multiplier applied to calculated walk speeds for ground Pets.
	 * Flying Pets use 1.0 instead since they need higher base speeds.
	 */
	private static final float BASE_SPEED_MULTIPLIER = 0.3f;

	/**
	 * Normal player walking speed (blocks/tick).
	 * Used as reference for calculating speed ratios when player has
	 * modified walk speed (via /speed command or effects).
	 */
	private static final float NORMAL_WALK_SPEED = 0.1f;

	/**
	 * Interval (in ticks) between look-at-owner updates.
	 * Controls how often the Pet turns its head toward the owner.
	 * Lower values = more responsive head tracking, higher CPU usage.
	 */
	private static final int LOOKAT_INTERVAL = 5;

	/**
	 * Squared distance constraint when owner is stationary.
	 * Must match {@link MyPetRandomStroll#STATIONARY_MAX_DIST_SQ} to prevent
	 * dead zones where neither FollowOwner nor RandomStroll activates.
	 * Value of 9.0 = 3 blocks squared.
	 */
	private static final double STATIONARY_MAX_DIST_SQ = 9.0;

	// ==================== INSTANCE FIELDS ====================

	private final EntityMyPet petEntity;
	private final AbstractNavigation nav;
	private final float stopDistance;
	private final double startDistance;
	private final float teleportDistance;
	private Control controlPathfinderGoal;
	private final Player owner;
	private boolean waitForGround = false;
	private final boolean flyingPet;

	private int lookAtTimer = 0;

	// Track owner position to calculate actual movement (getDeltaMovement is unreliable for players)
	private double lastOwnerX = 0;
	private double lastOwnerZ = 0;
	private double ownerMovementSpeed = 0; // Smoothed actual movement per tick
	private double ownerSpeedPeak = 0; // Peak speed with slow decay - prevents premature slowing
	private int lastTrackingTick = -1; // Detect gaps when FollowOwner wasn't active

	/**
	 * Creates a new FollowOwner AI goal for the specified Pet entity.
	 *
	 * @param entityMyPet       the Pet entity that will follow its owner
	 * @param startDistance     minimum distance before Pet starts following (will be squared internally)
	 * @param stopDistance      distance at which Pet stops following (will be squared internally)
	 * @param teleportDistance  distance at which Pet teleports to owner (will be squared internally)
	 */
	public FollowOwner(EntityMyPet entityMyPet, double startDistance, float stopDistance, float teleportDistance) {
		this.petEntity = entityMyPet;
		this.nav = entityMyPet.getPetNavigation();
		this.startDistance = startDistance * startDistance;
		this.stopDistance = stopDistance * stopDistance;
		this.teleportDistance = teleportDistance * teleportDistance;
		this.owner = ((CraftPlayer) petEntity.getOwner().getPlayer()).getHandle();
        this.flyingPet = entityMyPet instanceof EntityMyFlyingPet;
    }

    /**
     * Checks if the Pet is currently swimming (in water and uses aquatic navigation).
     *
     * <p>Unlike {@link #flyingPet}, this is evaluated dynamically since aquatic Pets
     * can transition between water and land.
     *
     * @return {@code true} if the Pet is in water and has aquatic navigation
     */
    private boolean isSwimmingPet() {
        return (this.petEntity.isInWater() || this.petEntity.getInBlockState().is(Blocks.BUBBLE_COLUMN))
                && this.petEntity.getNavigation() instanceof MyAquaticPetPathNavigation;
    }

    /**
     * Checks if the Pet has aquatic navigation capabilities.
     *
     * @return {@code true} if the Pet can navigate underwater
     */
    private boolean canPetSwim() {
        return this.petEntity.getNavigation() instanceof MyAquaticPetPathNavigation;
    }

    /**
     * Checks if the owner is in water while the Pet cannot swim.
     *
     * <p>When this condition is true, the Pet will use surface swimming behavior
     * to follow the owner horizontally rather than attempting underwater pathfinding.
     *
     * @return {@code true} if owner is in water and Pet lacks aquatic navigation
     */
    private boolean isOwnerInWaterAndPetCantSwim() {
        return owner.isInWater() && !canPetSwim() && !flyingPet;
    }

    /** Tracks whether the Pet is currently performing surface swimming toward the owner. */
    private boolean surfaceSwimmingToOwner = false;

	@Override
	public boolean shouldStart() {
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

        // Use tighter constraint when owner is stationary to eliminate dead zone with RandomStroll
        // ownerMovementSpeed might not be initialized yet on first tick, so also check position delta
        double distSqToOwner = this.petEntity.distanceToSqr(owner);
        double effectiveStartDistance = this.startDistance;
        if (ownerMovementSpeed < OWNER_STATIONARY_THRESHOLD) {
            // Owner is stationary - use same tight constraint as RandomStroll
            effectiveStartDistance = STATIONARY_MAX_DIST_SQ;
        }

        if (distSqToOwner < effectiveStartDistance) {
            return false;
        }
        return controlPathfinderGoal == null || controlPathfinderGoal.moveTo == null;
	}

	@Override
	public boolean shouldFinish() {
		if (controlPathfinderGoal != null && controlPathfinderGoal.moveTo != null) {
			return true;
		} else if (this.petEntity.getOwner() == null) {
            return true;
        }

        // Use tighter stop distance when owner is stationary
        double effectiveStopDistance = this.stopDistance;
        if (ownerMovementSpeed < OWNER_STATIONARY_THRESHOLD) {
            effectiveStopDistance = STATIONARY_MAX_DIST_SQ;
        }

        if (this.petEntity.distanceToSqr(owner) < effectiveStopDistance) {
			return true;
		} else if (!this.petEntity.canMove()) {
			return true;
		} else return this.petEntity.getMyPetTarget() != null && !this.petEntity.getMyPetTarget().isDead();
	}

	@Override
    public void start() {
        double distance = Math.sqrt(this.petEntity.distanceToSqr(owner));
		applyWalkSpeed(distance);
	}

	@Override
	public void finish() {
		nav.getParameters().removeSpeedModifier("FollowOwner");
		this.nav.stop();
	}

	@Override
    public void tick() {
        // Use NMS level check to avoid Location object allocations
        if (this.petEntity.level() != owner.level()) {
            return;
        }

        // Calculate actual owner movement by tracking position changes
        // This must happen early in tick() so ownerMovementSpeed is available for velocity logic
        double currentOwnerX = owner.getX();
        double currentOwnerZ = owner.getZ();
        int currentTick = petEntity.tickCount;
        int ticksSinceLastUpdate = currentTick - lastTrackingTick;
        lastTrackingTick = currentTick;

        // Only calculate movement if we have continuous tracking (no gaps)
        // Gaps occur when FollowOwner wasn't active (e.g., during RandomStroll)
        if (ticksSinceLastUpdate <= 2 && ticksSinceLastUpdate > 0) {
            double dx = currentOwnerX - lastOwnerX;
            double dz = currentOwnerZ - lastOwnerZ;
            double currentMovement = Math.sqrt(dx * dx + dz * dz);
            // Normalize to per-tick if multiple ticks passed
            if (ticksSinceLastUpdate > 1) {
                currentMovement /= ticksSinceLastUpdate;
            }
            // Use aggressive smoothing for stability (responsive vs smooth)
            ownerMovementSpeed = SPEED_SMOOTHING_RESPONSIVE * currentMovement + SPEED_SMOOTHING_STABLE * ownerMovementSpeed;
            // Peak tracking: slowly decay but jump up if current exceeds peak
            ownerSpeedPeak = ownerSpeedPeak * SPEED_PEAK_DECAY;
            if (ownerMovementSpeed > ownerSpeedPeak) {
                ownerSpeedPeak = ownerMovementSpeed;
            }
        }
        // Always update tracked position (even after gap, to prepare for next tick)
        lastOwnerX = currentOwnerX;
        lastOwnerZ = currentOwnerZ;

        double distanceSqr = this.petEntity.distanceToSqr(owner);
        double distance = Math.sqrt(distanceSqr);

		// Look at owner only every LOOKAT_INTERVAL ticks
		if (--lookAtTimer <= 0) {
			this.petEntity.getLookControl().setLookAt(owner, this.petEntity.getMaxHeadXRot(), this.petEntity.getMaxHeadXRot());
			lookAtTimer = LOOKAT_INTERVAL;
        }

        // Check if owner is in water (swimming/bobbing) and Pet cannot swim
        // Non-swimming Pets (zombies, etc.) should swim on the surface toward owner's
        // horizontal position instead of trying to dive down or teleporting repeatedly
        if (isOwnerInWaterAndPetCantSwim()) {
            if (!surfaceSwimmingToOwner) {
                surfaceSwimmingToOwner = true;
            }

            // Calculate horizontal distance to owner (ignore Y)
            double dx = owner.getX() - petEntity.getX();
            double dz = owner.getZ() - petEntity.getZ();
            double horizontalDistSq = dx * dx + dz * dz;
            double horizontalDist = Math.sqrt(horizontalDistSq);

            // If horizontally close enough, just float in place
            if (horizontalDist < SURFACE_SWIM_CLOSE) {
                return;
            }

            // Swim on the surface toward owner's horizontal position
            // Use velocity-based movement since we can't use normal pathfinding underwater
            Vec3 vel = petEntity.getDeltaMovement();

            // Normalize horizontal direction
            double dirX = dx / horizontalDist;
            double dirZ = dz / horizontalDist;

            // Surface swimming speed - scale with distance but cap it
            double swimSpeed = Math.min(SURFACE_SWIM_MAX_SPEED, SURFACE_SWIM_BASE_SPEED + horizontalDist * 0.01);

            // Apply horizontal velocity toward owner, keep existing Y velocity (buoyancy)
            double newVelX = dirX * swimSpeed;
            double newVelZ = dirZ * swimSpeed;

            // Blend with existing velocity for smoother movement
            newVelX = vel.x * SURFACE_SWIM_BLEND + newVelX * SURFACE_SWIM_BLEND;
            newVelZ = vel.z * SURFACE_SWIM_BLEND + newVelZ * SURFACE_SWIM_BLEND;

            Vec3 newVel = new Vec3(newVelX, vel.y, newVelZ);
            petEntity.setDeltaMovement(newVel);

            // Look at owner while swimming
            this.petEntity.getLookControl().setLookAt(owner, this.petEntity.getMaxHeadXRot(), this.petEntity.getMaxHeadXRot());

            // Don't do normal pathfinding or teleporting - we're handling movement ourselves
            return;
        } else if (surfaceSwimmingToOwner) {
            surfaceSwimmingToOwner = false;
		}

		//Teleportation
		if (this.petEntity.canMove()) {
			if ((!owner.getAbilities().flying && !owner.getBukkitEntity().isGliding()) || flyingPet) {
				if (!waitForGround) {
					if (owner.fallDistance <= 4 || flyingPet) {
                        if (distanceSqr >= this.teleportDistance) {
                            if (controlPathfinderGoal.moveTo == null && !petEntity.hasTarget()) {
                                Location ownerLocation = owner.getBukkitEntity().getLocation();
                                if (MyPetApi.getPlatformHelper().canSpawn(ownerLocation, this.petEntity)) {
                                    // Stop navigation and velocity before teleport
                                    this.nav.stop();
                                    this.petEntity.setDeltaMovement(Vec3.ZERO);
                                    // Teleport without collision checks
                                    this.petEntity.teleportTo(owner.getX(), owner.getY(), owner.getZ());
                                    this.petEntity.fallDistance = 0;
									return;
								}
							}
						}
					}
				} else if (owner.onGround()) {
					waitForGround = false;
				}
			} else {
                waitForGround = true;
            }

            // Pathfind every tick if owner has moved enough
            if (distanceSqr > FULL_STOP_DIST_SQ) {
                // Apply velocity control to ALL Pets to prevent overshooting
                // Ground Pets were previously too fast because walk speed doesn't limit actual velocity
                boolean swimmingPet = isSwimmingPet();
                {
                    Vec3 vel = this.petEntity.getDeltaMovement();
                    float ownerSpeed = owner.getAbilities().walkingSpeed;
                    // Cap speed multiplier - Pets can't handle extreme speeds
                    // At speed 5 (0.5 walkspeed), raw multiplier would be 5.0, cap at MAX_SPEED_MULTIPLIER
                    double rawSpeedMultiplier = Math.max(1.0, ownerSpeed / NORMAL_WALK_SPEED);
                    double speedMultiplier = Math.min(MAX_SPEED_MULTIPLIER, rawSpeedMultiplier);

                    // Y-control: Flying Pets should hover ABOVE the owner
                    // Ground/swimming Pets don't need this
                    if (flyingPet) {
                        double targetYLevel = owner.getY() + HOVER_HEIGHT;

                        // Calculate Y offset from TARGET hover position (not owner's Y)
                        double yDiff = targetYLevel - petEntity.getY(); // positive = need to go up, negative = need to go down
                        double maxYVel = 0.5 * Math.min(speedMultiplier, 2.0); // Cap Y at 0.5-1.0

                        // DIRECTIONAL Y CONTROL: Force Y velocity toward target hover height
                        // If bat is above target (yDiff < 0), Y velocity should be negative (down)
                        // If bat is below target (yDiff > 0), Y velocity should be positive (up)
                        double targetY;
                        if (Math.abs(yDiff) < 2.0) {
                            // Close to target height - gentle correction only
                            targetY = yDiff * 0.3; // Smooth approach
                        } else if (yDiff < 0) {
                            // Bat is ABOVE target - must go DOWN
                            // Cap to negative value, stronger when further above
                            targetY = Math.max(-maxYVel, yDiff * 0.15);
                            if (vel.y > 0) {
                                // WRONG DIRECTION! Override completely
                                targetY = -maxYVel * 0.8;
                            }
                        } else {
                            // Bat is BELOW target - go UP, but capped
                            targetY = Math.min(maxYVel, yDiff * 0.15);
                        }

                        // Apply Y correction: blend current Y with target
                        double newY = vel.y * 0.3 + targetY * 0.7;
                        newY = Math.max(-maxYVel, Math.min(maxYVel, newY)); // Hard cap both directions
                        vel = new Vec3(vel.x, newY, vel.z);
                        this.petEntity.setDeltaMovement(vel);
                    } // End of flyingPet Y-control block

                    double speed = vel.length();

                    // Velocity cap calculation - different margins for ground vs flying/swimming Pets
                    // Flying/swimming Pets have path inefficiency in 3D, so they need higher margins
                    // Ground Pets follow paths more directly, so margins should be closer to 1.0
                    boolean needsHighMargin = flyingPet || swimmingPet;

                    double maxVel;
                    if (distance < CRUISE_RANGE) {
                        // CRUISE MODE: Cap velocity proportional to owner speed
                        // This prevents over-acceleration and oscillation
                        double distFactor;
                        double margin;
                        if (distance < CLOSE_RANGE) {
                            // CLOSE: Move slower than owner to prevent catch-up
                            distFactor = 1.0;
                            margin = VEL_CAP_CLOSE_MARGIN;
                        } else if (distance < TRANSITION_RANGE) {
                            // TRANSITION: Gradual change
                            distFactor = 1.0;
                            if (needsHighMargin) {
                                margin = VEL_CAP_CLOSE_MARGIN + (distance - CLOSE_RANGE) * 0.35; // -> VEL_CAP_FAR_MARGIN_HIGH
                            } else {
                                margin = VEL_CAP_CLOSE_MARGIN + (distance - CLOSE_RANGE) * 0.05; // -> VEL_CAP_FAR_MARGIN_LOW
                            }
                        } else {
                            // CATCHING UP: Allow faster movement for flying/swimming only
                            if (needsHighMargin) {
                                distFactor = 1.0 + (distance - TRANSITION_RANGE) * 0.06; // 1.0 -> 1.3
                                margin = VEL_CAP_FAR_MARGIN_HIGH;
                            } else {
                                // Ground Pets: NO bonus, tight margin (pathfinder is efficient)
                                distFactor = 1.0;
                                margin = VEL_CAP_FAR_MARGIN_LOW;
                            }
                        }
                        maxVel = ownerMovementSpeed * distFactor * margin;
                        // Minimum cap to ensure Pet can still move when owner is slow/stationary
                        maxVel = Math.max(maxVel, needsHighMargin ? VEL_CAP_MIN_HIGH : VEL_CAP_MIN_LOW);
                    } else {
                        // FAR MODE: Use distance-based formula for rapid catch-up
                        double ownerBasedMinVel = ownerSpeedPeak * (needsHighMargin ? 1.3 : 1.1);
                        maxVel = Math.max(speedMultiplier, ownerBasedMinVel);
                    }

                    // Calculate direction to owner (horizontal for steering)
                    Vec3 toOwner = new Vec3(
                            owner.getX() - petEntity.getX(),
                            0,
                            owner.getZ() - petEntity.getZ()
                    );
                    Vec3 horizontalVel = new Vec3(vel.x, 0, vel.z);
                    double horizontalSpeed = horizontalVel.length();
                    double toOwnerDist = toOwner.length();

                    // Calculate alignment: dot / (|vel| * |toOwner|) = cos(angle)
                    // 1.0 = moving directly toward, 0.0 = perpendicular, -1.0 = moving away
                    double alignment = 0;
                    if (horizontalSpeed > 0.05 && toOwnerDist > 0.1) {
                        alignment = horizontalVel.dot(toOwner) / (horizontalSpeed * toOwnerDist);
                    }

                    // STEERING: Apply continuous steering toward owner based on alignment
                    // At higher speeds, start steering earlier (higher threshold) to prevent overshoot
                    // Base threshold: STEER_BASE_THRESHOLD (60°), at high speed: STEER_MAX_THRESHOLD (45°)
                    double steerThreshold = STEER_BASE_THRESHOLD + (speedMultiplier - 1.0) * 0.13;
                    steerThreshold = Math.min(STEER_MAX_THRESHOLD, steerThreshold);

                    if (horizontalSpeed > STEER_MIN_SPEED && toOwnerDist > STEER_MIN_DIST) {
                        Vec3 toOwnerNormalized = toOwner.normalize();

                        // Steering strength: stronger when more off-course and when closer
                        if (alignment < steerThreshold) {
                            // Calculate how much to steer: blend current velocity toward target direction
                            // At high speeds, steer more aggressively
                            double baseSteer = STEER_BASE_FACTOR + (speedMultiplier - 1.0) * 0.1;
                            double steerFactor = baseSteer + (steerThreshold - alignment) * 0.4;
                            steerFactor = Math.min(STEER_MAX_FACTOR, steerFactor);

                            // Blend current velocity with target direction
                            double keepFactor = 1.0 - steerFactor;
                            double newVelX = horizontalVel.x * keepFactor + toOwnerNormalized.x * horizontalSpeed * steerFactor;
                            double newVelZ = horizontalVel.z * keepFactor + toOwnerNormalized.z * horizontalSpeed * steerFactor;

                            Vec3 newVel = new Vec3(newVelX, vel.y, newVelZ);
                            this.petEntity.setDeltaMovement(newVel);
                            // Update vel for subsequent checks
                            vel = newVel;
                            speed = vel.length();
                            horizontalVel = new Vec3(vel.x, 0, vel.z);
                            horizontalSpeed = horizontalVel.length();
                        }
                    }

                    // BRAKE & VELOCITY CAP: Only for flying/swimming Pets (velocity-based movement)
                    // Ground Pets use position-based pathfinding with direct MOVEMENT_SPEED attribute control
                    if (flyingPet || swimmingPet) {
                        // BRAKE: When close and moving fast, slow down to prevent overshooting
                        // Scale down target speed as we get closer - below owner speed when very close
                        double brakeDistance = Math.max(BRAKE_MIN_DISTANCE, 5.0 / speedMultiplier);
                        // minBrakeSpeed scales: ownerSpeed at brakeDistance, BRAKE_FACTOR_MAX*ownerSpeed at dist=0
                        double distRatio = Math.min(1.0, distance / brakeDistance);
                        double minBrakeSpeed = ownerMovementSpeed * (BRAKE_FACTOR_MAX + (1.0 - BRAKE_FACTOR_MAX) * distRatio);
                        // CRITICAL: Brake minimum must not exceed velocity cap, otherwise brake overrides
                        // the close-range slowdown and Pet still catches up
                        minBrakeSpeed = Math.min(minBrakeSpeed, maxVel);
                        if (distance < brakeDistance && speed > BRAKE_SPEED_THRESHOLD && speed > minBrakeSpeed) {
                            // Progressive braking: stronger when closer
                            double brakeFactor = BRAKE_FACTOR_MIN + distRatio * (BRAKE_FACTOR_MAX - BRAKE_FACTOR_MIN);
                            double targetSpeed = speed * brakeFactor;
                            // Apply minimum but it now scales with distance
                            if (targetSpeed < minBrakeSpeed) {
                                brakeFactor = minBrakeSpeed / speed;
                                targetSpeed = minBrakeSpeed;
                            }
                            Vec3 newVel = vel.scale(brakeFactor);
                            this.petEntity.setDeltaMovement(newVel);
                        } else if (speed > maxVel) {
                            // Cap velocity to prevent runaway acceleration
                            Vec3 newVel = vel.normalize().scale(maxVel);
                            this.petEntity.setDeltaMovement(newVel);
                        }
                    }
                }
                if (this.nav.navigateTo(owner.getBukkitEntity())) {
                    applyWalkSpeed(distance);
                }
            } else {
                // Very close - stop navigation and clear momentum for all Pets
                this.nav.stop();
                this.petEntity.setDeltaMovement(Vec3.ZERO);
            }
        }
    }

    /**
     * Calculates and applies the appropriate walk speed based on distance to owner.
     *
     * <p>This method implements distance-based speed zones:
     * <ul>
     *   <li><b>Cruise Mode</b> ({@code distance < CRUISE_RANGE}): Speed proportional to owner's movement</li>
     *   <li><b>FAR Mode</b> ({@code distance >= CRUISE_RANGE}): Boosted speed for catch-up</li>
     * </ul>
     *
     * <p>Speed modifiers are applied differently based on Pet type:
     * <ul>
     *   <li>Flying/Swimming Pets: Higher multipliers due to velocity cap overhead</li>
     *   <li>Ground Pets: Lower multipliers with direct MOVEMENT_SPEED attribute control</li>
     * </ul>
     *
     * @param distance the current distance from Pet to owner in blocks
     */
    private void applyWalkSpeed(double distance) {
        float baseWalkSpeed = owner.getAbilities().walkingSpeed;
        float walkSpeed = baseWalkSpeed;

        // When close to owner, use cruise speed to match their pace
        // Extended range to prevent oscillation at boundary
        if (distance < CRUISE_RANGE) {
            // Flying/swimming Pets have velocity caps, so they need higher walk speeds
            // Ground Pets don't have velocity caps, so they need much lower multipliers
            boolean hasVelocityCap = flyingPet || isSwimmingPet();

            double distanceFactor;
            double speedMultiplier;

            if (hasVelocityCap) {
                // FLYING/SWIMMING PETS: Higher multipliers for snappier movement
                if (distance < CLOSE_RANGE) {
                    distanceFactor = 1.0;
                    speedMultiplier = FLY_SWIM_CRUISE_CLOSE_MULT;
                } else if (distance < TRANSITION_RANGE) {
                    distanceFactor = 1.0;
                    speedMultiplier = FLY_SWIM_CRUISE_CLOSE_MULT + (distance - CLOSE_RANGE); // -> FAR_MULT
                } else {
                    distanceFactor = 1.0 + (distance - TRANSITION_RANGE) * FLY_SWIM_CRUISE_DIST_FACTOR;
                    speedMultiplier = FLY_SWIM_CRUISE_FAR_MULT;
                }
            } else {
                // GROUND PETS: Direct MOVEMENT_SPEED attribute control (set below)
                // Use distance-based scaling to catch up when further away
                if (distance < CLOSE_RANGE) {
                    distanceFactor = 1.0;
                    speedMultiplier = GROUND_CRUISE_CLOSE_MULT;
                } else if (distance < TRANSITION_RANGE) {
                    distanceFactor = 1.0;
                    speedMultiplier = GROUND_CRUISE_CLOSE_MULT + (distance - CLOSE_RANGE) * GROUND_CRUISE_TRANSITION_SCALE;
                } else {
                    distanceFactor = 1.0 + (distance - TRANSITION_RANGE) * GROUND_CRUISE_DIST_FACTOR;
                    speedMultiplier = GROUND_CRUISE_FAR_MULT;
                }
            }

            // For ground Pets, use ownerSpeedPeak (fast-responding) instead of smoothed ownerMovementSpeed
            // This prevents the Pet from falling behind when owner starts walking
            double speedForCruise = (flyingPet || isSwimmingPet()) ? ownerMovementSpeed : ownerSpeedPeak;
            float cruiseSpeed = (float) (speedForCruise * speedMultiplier * distanceFactor);

            if (flyingPet && distance >= CLOSE_RANGE) {
                // Flying bonus only when not in close range
                cruiseSpeed += FLYING_BONUS_SPEED;
            }
            // Swimming Pets need significantly higher speed in water due to water resistance
            if (isSwimmingPet()) {
                cruiseSpeed += SWIMMING_BONUS_SPEED;
                if (owner.hasEffect(MobEffects.DOLPHINS_GRACE)) {
                    cruiseSpeed += DOLPHINS_GRACE_BONUS;
                }
            }

            // Flying/swimming Pets need higher minimum due to 3D pathing overhead
            // Ground Pets need reasonable minimum to not feel sluggish
            float minCruise = (flyingPet || isSwimmingPet()) ? FLY_SWIM_MIN_CRUISE : GROUND_MIN_CRUISE;
            cruiseSpeed = Math.max(minCruise, cruiseSpeed); // Minimum to keep moving
            nav.getParameters().addSpeedModifier("FollowOwner", cruiseSpeed);

            // For ground Pets, directly set entity's MOVEMENT_SPEED attribute every tick
            // Nav parameter changes only apply when starting a new path, not during ongoing navigation
            if (!flyingPet && !isSwimmingPet()) {
                petEntity.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(cruiseSpeed);
            }

            return;
        }

        // FAR mode: distance >= 10 blocks, need to catch up quickly
		if (owner.getAbilities().flying) {
			// make the Pet faster when the player is flying
			walkSpeed += owner.getAbilities().flyingSpeed;
		} else if (owner.isSprinting() || owner.getBukkitEntity().isGliding()) {
			// make the Pet faster when the player is sprinting
            AttributeInstance speedAttribute = owner.getAttributes().getInstance(Attributes.MOVEMENT_SPEED);
            if (speedAttribute != null) {
                walkSpeed += (float) speedAttribute.getValue() - 0.037f;
            }
        } else if (owner.isPassenger() && owner.getVehicle() instanceof LivingEntity livingVehicle) {
			// adjust the speed to the Pet can catch up with the vehicle the player is in
            AttributeInstance vehicleSpeedAttribute = livingVehicle.getAttributes().getInstance(Attributes.MOVEMENT_SPEED);
			if (vehicleSpeedAttribute != null) {
				walkSpeed = (float) vehicleSpeedAttribute.getValue();
			}
		}

		// make aquatic/flying Pets faster
		// This is actually due to there being a difference between MovementSpeed and FlyingSpeed, FlyingSpeed being higher
		// (I don't completely know why this is the case for swimming but I imagine it has a similar reason)
		if((this.petEntity.isInWater() || this.petEntity.getInBlockState().is(Blocks.BUBBLE_COLUMN)) && this.petEntity.getNavigation() instanceof MyAquaticPetPathNavigation) {
			walkSpeed += 0.6f;
			if(owner.hasEffect(MobEffects.DOLPHINS_GRACE)) {
				walkSpeed += 0.08f;
			}
		}

		if(this.petEntity.getMoveControl() instanceof MyPetFlyingMoveControl) {
			walkSpeed += 0.575f;
			if(owner.isSprinting()) {
                walkSpeed -= 0.05f;
            }
        }

        // Dynamic speed boost based on distance:
        // - Close (< TRANSITION_RANGE blocks): match player speed exactly
        // - Far (> CRUISE_RANGE blocks): full boost to catch up
        // - In between: gradual increase
        float speedBoost = 0f;
        if (distance > TRANSITION_RANGE) {
            // Scale from 0 to FAR_SPEED_BOOST_MAX as distance increases
            speedBoost = (float) Math.min(FAR_SPEED_BOOST_MAX, (distance - TRANSITION_RANGE) * FAR_SPEED_BOOST_SCALE);
        }
        walkSpeed += speedBoost;

        // Dynamic multiplier: scale UP when player moves faster than normal walking speed
        // Only use baseWalkSpeed (not flying bonuses) for ratio to avoid exponential growth
        float speedRatio = Math.max(1.0f, baseWalkSpeed / NORMAL_WALK_SPEED);
        // Flying Pets need higher base multiplier since their bonuses get reduced too much
        float baseMultiplier = flyingPet ? 1.0f : BASE_SPEED_MULTIPLIER;
        float effectiveMultiplier = baseMultiplier * speedRatio;
        // Cap multiplier to prevent runaway speed
        effectiveMultiplier = Math.min(FAR_MAX_MULTIPLIER, effectiveMultiplier);

        // For flying Pets at extreme player speeds, cap the walk speed to prevent
        // the pathfinder from over-accelerating (which then gets slammed by velocity cap)
        // This creates smoother motion by letting the pathfinder and velocity cap work together
        if (flyingPet && walkSpeed > FLYING_WALK_SPEED_CAP) {
            walkSpeed = FLYING_WALK_SPEED_CAP;
        }

        // Apply the dynamic multiplier
        float finalSpeed = walkSpeed * effectiveMultiplier;

        nav.getParameters().addSpeedModifier("FollowOwner", finalSpeed);
	}
}

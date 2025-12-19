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

import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_21_R1.entity.EntityMyPet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.ai.util.RandomPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;

/**
 * AI goal that makes aquatic Pets swim randomly near their stationary owner.
 *
 * <p>This goal extends {@link MyPetRandomStroll} to provide specialized underwater wandering
 * behavior for aquatic Pets (fish, dolphins, axolotls, etc.). Aquatic Pets have a moderate
 * chance of wandering (6% versus 2%) and use water-pathfindable position generation.
 *
 * <p>Key differences from ground strolling:
 * <ul>
 *   <li>Uses water-pathfindable position generation when both Pet and owner are in water</li>
 *   <li>Larger wander radius (3-10 blocks horizontal, 2-7 vertical) for natural swimming</li>
 *   <li>Higher wander chance (6%) for more active swimming behavior</li>
 *   <li>Faster speeds (0.25-0.35) to account for water resistance</li>
 *   <li>Falls back to land strolling when Pet is not in water</li>
 * </ul>
 *
 * @see MyPetRandomStroll
 * @see MyPetRandomFly
 */
@Compat("v1_21_R1")
public class MyPetRandomSwim extends MyPetRandomStroll {

    // ==================== TUNABLE CONSTANTS ====================
    // These values control aquatic Pet random swim behavior.
    // All distance values are in blocks, speeds are in blocks/tick unless noted.

    // -------------------------------------------------------------------------
    // SWIM ACTIVATION - Parameters controlling when swimming starts
    // -------------------------------------------------------------------------

    /**
     * Probability (0.0-1.0) of starting a swim each tick when conditions are met.
     * Value of 0.06 means 6% chance per tick - 3x higher than ground Pets.
     * Creates moderately active swimming appropriate for aquatic creatures.
     */
    private static final float SWIM_STROLL_CHANCE = 0.06F;

    // -------------------------------------------------------------------------
    // SWIM POSITION GENERATION - Parameters for underwater target selection
    // -------------------------------------------------------------------------

    /**
     * Horizontal swim radius (blocks) when owner is stationary.
     * Aquatic Pets use slightly larger radius than flying Pets since
     * water environments tend to be more open.
     */
    private static final int SWIM_WANDER_RADIUS_STATIONARY = 3;

    /**
     * Horizontal swim radius (blocks) when owner is moving.
     * Large radius (10 blocks) allows natural long-distance swimming.
     */
    private static final int SWIM_WANDER_RADIUS_MOVING = 10;

    /**
     * Vertical swim radius (blocks) when owner is stationary.
     * Controls depth variation during idle swimming.
     */
    private static final int SWIM_WANDER_VERTICAL_STATIONARY = 2;

    /**
     * Vertical swim radius (blocks) when owner is moving.
     * Large radius (7 blocks) allows natural depth changes.
     */
    private static final int SWIM_WANDER_VERTICAL_MOVING = 7;

    /**
     * Maximum squared distance (blocks²) from Pet's current position for swim targets when stationary.
     * Value of 9.0 = 3 blocks. Keeps swimming contained.
     */
    private static final double SWIM_WANDER_MAX_DIST_SQ_FROM_PET_STATIONARY = 9.0;

    /**
     * Maximum squared distance (blocks²) from Pet's current position when owner is moving.
     * Value of 16.0 = 4 blocks. Allows longer swim distances.
     */
    private static final double SWIM_WANDER_MAX_DIST_SQ_FROM_PET_MOVING = 16.0;

    /**
     * Maximum attempts to find a valid swim position before giving up.
     * Same as other Pet types for consistency.
     */
    private static final int SWIM_WANDER_MAX_ATTEMPTS = 5;

    /**
     * Maximum iterations for water position validation in {@link #makeWaterPos}.
     * Higher than wander attempts because water pathfinding is more constrained.
     */
    private static final int WATER_POS_MAX_ITERATIONS = 10;

    // -------------------------------------------------------------------------
    // SWIM SPEED - Parameters for underwater movement
    // -------------------------------------------------------------------------

    /**
     * Movement speed (blocks/tick) for swimming when owner is stationary.
     * Higher than ground Pets (0.15) to compensate for water drag.
     */
    private static final double SWIM_SPEED_STATIONARY = 0.25;

    /**
     * Movement speed (blocks/tick) for swimming when owner is moving.
     * Higher than stationary to keep up, but still relaxed.
     */
    private static final double SWIM_SPEED_MOVING = 0.35;

    /**
     * Creates a new random swim AI goal for the specified aquatic Pet.
     *
     * @param petEntity     the aquatic Pet entity
     * @param startDistance maximum distance from owner for swimming (will be squared by parent)
     */
    public MyPetRandomSwim(EntityMyPet petEntity, int startDistance) {
        super(petEntity, startDistance);
        this.strollChance = SWIM_STROLL_CHANCE;
    }

    /**
     * Generates a random underwater position for the aquatic Pet to swim toward.
     *
     * <p>When both the Pet and owner are in water, uses specialized water position
     * generation with larger radii (3-10 horizontal, 2-7 vertical when owner is moving).
     * Falls back to parent's land position generation when not in water.
     *
     * @return a valid swim target position, or {@code null} if no suitable position found
     */
    @Override
    protected Vec3 getPosition() {
        boolean petInWater = petEntity.isInWater() || petEntity.getInBlockState().is(Blocks.BUBBLE_COLUMN);
        boolean ownerInWater = owner.isInWater() || owner.getInBlockState().is(Blocks.BUBBLE_COLUMN);

        // ownerStationary is already set by parent's updateOwnerMovementTracking() in shouldStart()
        int horizontalRadius = ownerStationary ? SWIM_WANDER_RADIUS_STATIONARY : SWIM_WANDER_RADIUS_MOVING;
        int verticalRadius = ownerStationary ? SWIM_WANDER_VERTICAL_STATIONARY : SWIM_WANDER_VERTICAL_MOVING;
        double maxDistSqFromOwner = ownerStationary ? STATIONARY_MAX_DIST_SQ : (double) startDistance;
        double maxDistSqFromPet = ownerStationary ? SWIM_WANDER_MAX_DIST_SQ_FROM_PET_STATIONARY : SWIM_WANDER_MAX_DIST_SQ_FROM_PET_MOVING;

        Vec3 petPos = petEntity.position();

        if (petInWater && ownerInWater) {
            for (int i = 0; i < SWIM_WANDER_MAX_ATTEMPTS; i++) {
                Vec3 candidate = makeWaterPos(this.petEntity, horizontalRadius, verticalRadius);
                if (candidate != null) {
                    double distSqToOwner = candidate.distanceToSqr(owner.getX(), owner.getY(), owner.getZ());
                    double distSqToPet = candidate.distanceToSqr(petPos);

                    if (distSqToOwner < maxDistSqFromOwner && distSqToPet < maxDistSqFromPet) {
                        return candidate;
                    }
                }
            }
            return null;
        }
        return super.getPosition();
    }

    /**
     * Generates a water-pathfindable position for swimming.
     *
     * <p>Attempts up to 10 iterations to find a position that is pathfindable through water
     * and has liquid above it (ensuring the Pet can actually swim there).
     *
     * @param mob              the pathfinder mob to generate position for
     * @param horizontalRadius horizontal search radius
     * @param verticalRadius   vertical search radius
     * @return a water-pathfindable position, or the last generated position
     */
    private Vec3 makeWaterPos(PathfinderMob mob, int horizontalRadius, int verticalRadius) {
        Vec3 position = generateRandomPosition(mob, horizontalRadius, verticalRadius);
        if (position == null) {
            return null;
        }

        for (int attempt = 0; attempt < WATER_POS_MAX_ITERATIONS; attempt++) {
            BlockPos blockPos = new BlockPos((int) position.x, (int) position.y, (int) position.z);
            BlockPos abovePos = blockPos.above();

            boolean isWaterPathfindable = mob.level().getBlockState(blockPos).isPathfindable(PathComputationType.WATER);
            boolean hasLiquidAbove = !mob.level().getFluidState(abovePos).isEmpty();

            if (isWaterPathfindable || hasLiquidAbove) {
                return position;
            }

            position = generateRandomPosition(mob, horizontalRadius, verticalRadius);
            if (position == null) {
                return null;
            }
        }

        return position;
    }

    /**
     * Generates a random position using the NMS random position utilities.
     *
     * @param mob              the pathfinder mob
     * @param horizontalRadius horizontal search radius
     * @param verticalRadius   vertical search radius
     * @return a randomly generated position vector
     */
    private static Vec3 generateRandomPosition(PathfinderMob mob, int horizontalRadius, int verticalRadius) {
        return RandomPos.generateRandomPos(mob, () -> {
            BlockPos blockPos = RandomPos.generateRandomDirection(mob.getRandom(), horizontalRadius, verticalRadius);
            return generateRandomPosTowardDirection(mob, horizontalRadius, GoalUtils.mobRestricted(mob, horizontalRadius), blockPos);
        });
    }

    /**
     * Validates and returns a position toward the given direction if it meets all constraints.
     *
     * @param entitycreature the pathfinder mob
     * @param i              search radius
     * @param flag           whether the mob is restricted
     * @param blockposition  target direction
     * @return the validated position, or {@code null} if invalid
     */
    private static BlockPos generateRandomPosTowardDirection(PathfinderMob entitycreature, int i, boolean flag, BlockPos blockposition) {
        BlockPos lePos = RandomPos.generateRandomPosTowardDirection(entitycreature, i, entitycreature.getRandom(), blockposition);
        return !GoalUtils.isOutsideLimits(lePos, entitycreature) && !GoalUtils.isRestricted(flag, entitycreature, lePos) && !GoalUtils.isNotStable(entitycreature.getNavigation(), lePos) ? lePos : null;
    }

    /**
     * Applies the movement speed for idle swimming.
     *
     * <p>When in water, uses faster speeds than ground Pets to compensate for water resistance:
     * <ul>
     *   <li>Stationary owner: 0.25 (calm swimming)</li>
     *   <li>Moving owner: 0.35 (active swimming)</li>
     * </ul>
     *
     * <p>Falls back to parent's land speed when the Pet is not in water.
     */
    @Override
    protected void applySpeed() {
        boolean petInWater = petEntity.isInWater() || petEntity.getInBlockState().is(Blocks.BUBBLE_COLUMN);

        if (petInWater) {
            double walkSpeed = ownerStationary ? SWIM_SPEED_STATIONARY : SWIM_SPEED_MOVING;
            nav.getParameters().addSpeedModifier("RandomStroll", walkSpeed);
        } else {
            super.applySpeed();
        }
    }
}

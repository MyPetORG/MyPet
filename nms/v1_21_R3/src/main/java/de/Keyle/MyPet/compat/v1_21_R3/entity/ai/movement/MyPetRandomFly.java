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

package de.Keyle.MyPet.compat.v1_21_R3.entity.ai.movement;

import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_21_R3.entity.EntityMyPet;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.phys.Vec3;

/**
 * AI goal that makes flying Pets hover and flutter randomly near their stationary owner.
 *
 * <p>This goal extends {@link MyPetRandomStroll} to provide specialized aerial wandering
 * behavior for flying Pets (bats, allays, bees, etc.). Flying Pets have a higher chance
 * of wandering (8% versus 2%) to create more active hovering behavior.
 *
 * <p>Key differences from ground strolling:
 * <ul>
 *   <li>Uses 3D position generation via {@link HoverRandomPos} and {@link AirAndWaterRandomPos}</li>
 *   <li>Supports both horizontal and vertical radius for wander targets</li>
 *   <li>Higher wander chance (8%) for more active buzzing/hovering</li>
 *   <li>Slightly faster movement speeds than ground Pets</li>
 * </ul>
 *
 * @see MyPetRandomStroll
 * @see MyPetRandomSwim
 */
@Compat("v1_21_R3")
public class MyPetRandomFly extends MyPetRandomStroll {

    // ==================== TUNABLE CONSTANTS ====================
    // These values control flying Pet random hover behavior.
    // All distance values are in blocks, speeds are in blocks/tick unless noted.

    // -------------------------------------------------------------------------
    // HOVER ACTIVATION - Parameters controlling when hovering starts
    // -------------------------------------------------------------------------

    /**
     * Probability (0.0-1.0) of starting a hover each tick when conditions are met.
     * Value of 0.08 means 8% chance per tick - 4x higher than ground Pets.
     * Creates more active buzzing/hovering behavior appropriate for flying creatures.
     */
    private static final float FLY_STROLL_CHANCE = 0.08F;

    // -------------------------------------------------------------------------
    // HOVER POSITION GENERATION - Parameters for aerial target selection
    // -------------------------------------------------------------------------

    /**
     * Horizontal hover radius (blocks) when owner is stationary.
     * Flying Pets use smaller radius for tighter hovering near owner.
     */
    private static final int FLY_WANDER_RADIUS_STATIONARY = 2;

    /**
     * Horizontal hover radius (blocks) when owner is moving.
     * Allows more freedom in position selection when following.
     */
    private static final int FLY_WANDER_RADIUS_MOVING = 4;

    /**
     * Vertical hover radius (blocks) when owner is stationary.
     * Controls altitude variation during idle hovering.
     */
    private static final int FLY_WANDER_VERTICAL_STATIONARY = 2;

    /**
     * Vertical hover radius (blocks) when owner is moving.
     * Allows more vertical freedom when following.
     */
    private static final int FLY_WANDER_VERTICAL_MOVING = 4;

    /**
     * Maximum squared distance (blocks²) from Pet's current position for hover targets when stationary.
     * Value of 9.0 = 3 blocks. Keeps hovering tight.
     */
    private static final double FLY_WANDER_MAX_DIST_SQ_FROM_PET_STATIONARY = 9.0;

    /**
     * Maximum squared distance (blocks²) from Pet's current position when owner is moving.
     * Value of 16.0 = 4 blocks. Allows longer hover distances.
     */
    private static final double FLY_WANDER_MAX_DIST_SQ_FROM_PET_MOVING = 16.0;

    /**
     * Maximum attempts to find a valid hover position before giving up.
     * Same as ground Pets for consistency.
     */
    private static final int FLY_WANDER_MAX_ATTEMPTS = 5;

    // -------------------------------------------------------------------------
    // HOVER SPEED - Parameters for aerial movement
    // -------------------------------------------------------------------------

    /**
     * Movement speed (blocks/tick) for hovering when owner is stationary.
     * Slow, calm hovering creates natural idle behavior.
     */
    private static final double FLY_HOVER_SPEED_STATIONARY = 0.2;

    /**
     * Movement speed (blocks/tick) for hovering when owner is moving.
     * Slightly faster to keep up while still appearing relaxed.
     */
    private static final double FLY_HOVER_SPEED_MOVING = 0.3;

    /**
     * Creates a new random fly AI goal for the specified flying Pet.
     *
     * @param petEntity     the flying Pet entity
     * @param startDistance maximum distance from owner for flying (will be squared by parent)
     */
    public MyPetRandomFly(EntityMyPet petEntity, int startDistance) {
        super(petEntity, startDistance);
        this.strollChance = FLY_STROLL_CHANCE;
    }

    /**
     * Generates a random aerial position for the flying Pet to hover toward.
     *
     * <p>Uses {@link HoverRandomPos} as the primary position generator, falling back to
     * {@link AirAndWaterRandomPos} if hover position fails. The position is constrained
     * within range of both the owner and the Pet's current position.
     *
     * <p>When the owner is stationary, uses a smaller radius (2 blocks vs 4 blocks)
     * for tighter hovering behavior.
     *
     * @return a valid aerial wander target, or {@code null} if no suitable position found
     */
    @Override
    protected Vec3 getPosition() {
        // ownerStationary is already set by parent's updateOwnerMovementTracking() in shouldStart()
        int horizontalRadius = ownerStationary ? FLY_WANDER_RADIUS_STATIONARY : FLY_WANDER_RADIUS_MOVING;
        int verticalRadius = ownerStationary ? FLY_WANDER_VERTICAL_STATIONARY : FLY_WANDER_VERTICAL_MOVING;
        double maxDistSqFromOwner = ownerStationary ? STATIONARY_MAX_DIST_SQ : (double) startDistance;
        double maxDistSqFromPet = ownerStationary ? FLY_WANDER_MAX_DIST_SQ_FROM_PET_STATIONARY : FLY_WANDER_MAX_DIST_SQ_FROM_PET_MOVING;

        Vec3 petPos = petEntity.position();
        Vec3 vec3d = this.petEntity.getViewVector(0.0F);

        for (int i = 0; i < FLY_WANDER_MAX_ATTEMPTS; i++) {
            Vec3 candidate = HoverRandomPos.getPos(this.petEntity, horizontalRadius, verticalRadius, vec3d.x, vec3d.z, 1.5707964F, 3, 1);
            if (candidate == null) {
                candidate = AirAndWaterRandomPos.getPos(this.petEntity, horizontalRadius, verticalRadius, -2, vec3d.x, vec3d.z, 1.5707963705062866D);
            }

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

    /**
     * Applies the movement speed for idle flying.
     *
     * <p>Uses calm, controlled speeds to prevent darting behavior:
     * <ul>
     *   <li>Stationary owner: 0.2 (slow hovering)</li>
     *   <li>Moving owner: 0.3 (slightly faster)</li>
     * </ul>
     */
    @Override
    protected void applySpeed() {
        double walkSpeed = ownerStationary ? FLY_HOVER_SPEED_STATIONARY : FLY_HOVER_SPEED_MOVING;
        nav.getParameters().addSpeedModifier("RandomStroll", walkSpeed);
    }
}

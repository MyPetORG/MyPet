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

package de.Keyle.MyPet.entity;

import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Mob;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/**
 * Shared spider-climb physics for pets — the wall probe and climb speed used by both the ridden
 * Ride-skill climb ({@code RideSkillFlightController}) and the autonomous Climb-skill goal.
 */
public final class PetClimbSupport {

    /** Upward climb velocity (blocks/tick) — ~4 m/s, a touch faster than a vanilla ladder. */
    public static final double CLIMB_SPEED = 0.2;
    /** How far ahead (blocks) to probe for a wall in the movement direction. */
    public static final double CLIMB_WALL_PROBE = 0.1;
    private static final double DEFAULT_STEP_HEIGHT = 0.6;

    private PetClimbSupport() {
    }

    /**
     * True if the mob's bounding box, shifted {@link #CLIMB_WALL_PROBE} blocks along
     * ({@code dirX}, {@code dirZ}), collides with the world above its step-height slice — i.e. a real
     * wall it can't just step over. Mounts no taller than their own step height never report a wall
     * (a slab/step they'd step up anyway).
     */
    public static boolean isWallAhead(Mob mob, double dirX, double dirZ) {
        double length = Math.hypot(dirX, dirZ);
        if (length < 1.0E-4) {
            return false;
        }
        double shiftX = dirX / length * CLIMB_WALL_PROBE;
        double shiftZ = dirZ / length * CLIMB_WALL_PROBE;
        AttributeInstance stepAttr = mob.getAttribute(PetAttributes.STEP_HEIGHT);
        double stepHeight = stepAttr != null ? stepAttr.getValue() : DEFAULT_STEP_HEIGHT;
        BoundingBox box = mob.getBoundingBox().shift(shiftX, 0, shiftZ);
        double climbFloor = box.getMinY() + stepHeight + 0.001;
        if (climbFloor >= box.getMaxY() - 0.001) {
            return false;
        }
        box = BoundingBox.of(
                new Vector(box.getMinX(), climbFloor, box.getMinZ()),
                new Vector(box.getMaxX(), box.getMaxY(), box.getMaxZ()));
        return mob.wouldCollideUsing(box);
    }
}

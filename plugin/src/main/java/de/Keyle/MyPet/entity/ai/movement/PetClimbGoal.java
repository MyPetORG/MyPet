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
import de.Keyle.MyPet.entity.PetClimbSupport;
import de.Keyle.MyPet.entity.ai.PetGoalKey;
import de.Keyle.MyPet.entity.ai.PetGoalWorlds;
import de.Keyle.MyPet.skill.skills.ClimbImpl;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * Autonomous spider-style wall climbing driven by the Climb skill. While the pet is purposefully
 * heading somewhere — its combat target, else its owner — that sits higher and beyond a wall it's
 * pressed against, it scales the wall straight up (a shortcut) instead of pathing all the way around,
 * then hands back to normal navigation once it crests. Idle wandering never triggers it.
 *
 * <p>Declares no {@link GoalType} so it runs concurrently with the follow/target goals — like
 * {@link PetFloatGoal}, it just overrides velocity for the ticks it's actually climbing.
 */
public class PetClimbGoal implements Goal<Mob> {

    /**
     * The destination must be at least this many blocks higher for climbing to be worth it — one full
     * block, so a pet in a hole (its owner up at the rim) climbs out, but it won't scale a wall to reach
     * something at its own level.
     */
    private static final double MIN_HEIGHT = 1.0;
    /** Ignore destinations farther than this (squared) horizontally — a distant target isn't "over this wall". */
    private static final double MAX_HORIZONTAL_SQUARED = 24 * 24;
    /** Below this horizontal distance the destination is treated as "straight up" (pet in a pit under it). */
    private static final double VERTICAL_HORIZONTAL = 0.4;
    /** Gentle horizontal nudge toward the wall while climbing (and over the top when it ends). */
    private static final double CLIMB_PUSH = 0.12;
    /** Light upward lift during the mantle so the pet drifts onto the ledge rather than falling straight back. */
    private static final double MANTLE_UP = 0.1;
    /** Ticks of gentle mantling after the wall ends — a soft continuation of the climb, not a shove. */
    private static final int MANTLE_TICKS = 8;
    /** Cardinal directions probed when the pet is boxed in directly under its destination. */
    private static final int[][] CARDINALS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    /** Safety cap: give up after ~5s of climbing so a pet can never get stuck rising forever. */
    private static final int MAX_CLIMB_TICKS = 100;

    private final Pet pet;
    private final Mob mob;
    private int climbTicks;
    /** Ticks left in the "mantle" phase (topping the ledge) after the wall stops being detected. */
    private int mantleTicks;
    /** The horizontal direction of the wall being climbed, reused to shove over the top. */
    private Vector climbDir;

    public PetClimbGoal(Pet pet, Mob mob) {
        this.pet = pet;
        this.mob = mob;
    }

    @Override
    public boolean shouldActivate() {
        if (!Bukkit.isOwnedByCurrentRegion(mob)) {
            return false;
        }
        if (!pet.getSkills().isActive(ClimbImpl.class)) {
            return false;
        }
        if (pet.isSitting() || !mob.getPassengers().isEmpty()) {
            return false; // ridden climbing is the Ride skill's job; a sitting pet stays put
        }
        if (pet.getPetType().isFlyingPet()) {
            return false; // flying pets fly over walls, they don't climb
        }
        Location destination = destination();
        return destination != null && climbable(destination);
    }

    @Override
    public boolean shouldStayActive() {
        if (!Bukkit.isOwnedByCurrentRegion(mob) || climbTicks >= MAX_CLIMB_TICKS) {
            return false;
        }
        if (pet.isSitting() || !mob.getPassengers().isEmpty()) {
            return false;
        }
        // Stay active while there's still a wall to climb, or while finishing the mantle over the top.
        Location destination = destination();
        return mantleTicks > 0 || (destination != null && climbable(destination));
    }

    @Override
    public void start() {
        climbTicks = 0;
        mantleTicks = 0;
        climbDir = null;
    }

    @Override
    public void stop() {
        climbTicks = 0;
        mantleTicks = 0;
        climbDir = null;
    }

    @Override
    public void tick() {
        climbTicks++;
        Location destination = destination();
        Vector dir = destination != null ? climbDirection(destination) : null;
        if (dir != null) {
            // Still against the wall: rise, pressing gently into it. Arm the mantle for when it ends.
            climbDir = dir;
            mantleTicks = MANTLE_TICKS;
            mob.setVelocity(new Vector(dir.getX() * CLIMB_PUSH, PetClimbSupport.CLIMB_SPEED, dir.getZ() * CLIMB_PUSH));
            mob.setFallDistance(0f);
        } else if (mantleTicks > 0 && climbDir != null) {
            // Wall's gone — we've reached the top. Keep the same soft forward nudge with a light lift so
            // the pet drifts over the lip and settles on the ledge, rather than sliding back down the face.
            mantleTicks--;
            mob.setVelocity(new Vector(climbDir.getX() * CLIMB_PUSH, MANTLE_UP, climbDir.getZ() * CLIMB_PUSH));
            mob.setFallDistance(0f);
        }
    }

    /** Where the pet is purposefully headed: its combat target, else its owner. Null if neither applies. */
    private Location destination() {
        if (pet.hasTarget()) {
            LivingEntity target = pet.getPetTarget();
            if (target != null && !target.isDead() && !PetGoalWorlds.isCrossWorld(mob, target)) {
                return target.getLocation();
            }
        }
        Player owner = pet.getOwner() != null ? pet.getOwner().getPlayer() : null;
        if (owner != null && owner.isOnline() && owner.getWorld() == mob.getWorld()) {
            return owner.getLocation();
        }
        return null;
    }

    /** True if {@code destination} is meaningfully higher, close enough horizontally, and a wall blocks the way. */
    private boolean climbable(Location destination) {
        if (destination.getY() - mob.getY() < MIN_HEIGHT) {
            return false;
        }
        double dx = destination.getX() - mob.getX();
        double dz = destination.getZ() - mob.getZ();
        if (dx * dx + dz * dz > MAX_HORIZONTAL_SQUARED) {
            return false;
        }
        return climbDirection(destination) != null;
    }

    /**
     * The horizontal direction of a wall the pet should climb to make progress toward {@code destination}:
     * toward the destination when a wall blocks that way, or — when the destination is essentially straight
     * up (the pet is boxed into a pit under it) — any walled side, so it can climb out. Null if no wall.
     */
    private Vector climbDirection(Location destination) {
        double dx = destination.getX() - mob.getX();
        double dz = destination.getZ() - mob.getZ();
        double horizontal = Math.hypot(dx, dz);
        if (horizontal >= VERTICAL_HORIZONTAL) {
            return PetClimbSupport.isWallAhead(mob, dx, dz)
                    ? new Vector(dx / horizontal, 0, dz / horizontal) : null;
        }
        // Nearly straight up — pick any walled side to climb out of the pit.
        for (int[] cardinal : CARDINALS) {
            if (PetClimbSupport.isWallAhead(mob, cardinal[0], cardinal[1])) {
                return new Vector(cardinal[0], 0, cardinal[1]);
            }
        }
        return null;
    }

    @Override
    public @NotNull GoalKey<Mob> getKey() {
        return PetGoalKey.CLIMB;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.noneOf(GoalType.class);
    }
}

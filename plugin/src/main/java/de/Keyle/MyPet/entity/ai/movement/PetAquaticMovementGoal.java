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
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * Paper Goal that supplements the default ground MoveControl with Y-axis velocity
 * and body rotation for aquatic pets in water.
 *
 * <p>The default MoveControl handles X/Z movement via pathfinder waypoints but cannot
 * produce Y-axis velocity underwater. This goal fills that gap by:
 * <ul>
 *   <li>Applying direct Y velocity toward the navigation target when swimming</li>
 *   <li>Syncing body rotation to face the swimming direction</li>
 *   <li>Applying gentle sinking when idle in water</li>
 * </ul>
 *
 * <p>On land it drives the pet horizontally toward its owner, because a stranded
 * water-breather has no movement of its own: vanilla's
 * {@code WaterBoundPathNavigation#canUpdatePath} returns
 * {@code allowBreaching || mob.isInLiquid()}, so a dry fish never follows its path,
 * and {@code AbstractFish.FishMoveControl} then falls into its {@code else} branch and
 * calls {@code setSpeed(0)} — which, via {@code Mob#setSpeed} → {@code setZza}, zeroes
 * the forward input {@code LivingEntity#travelInAir} would have used. The only force
 * left acting on it is the random flop impulse in {@code AbstractFish#aiStep}, so the
 * pet hops in place until the follow goal gives up and teleports it.
 *
 * <p>The Y component is deliberately left untouched on land: goals tick inside
 * {@code serverAiStep()}, which runs after that flop impulse and before
 * {@code travel()}, so preserving Y keeps the vanilla flop hop (and the flop sound)
 * intact while replacing its random horizontal nudge with a heading toward the owner.
 * The hop also doubles as the pet's only way over a block — {@code FishMoveControl}
 * has no jump branch.
 *
 * <p>Uses {@code GoalType.UNKNOWN_BEHAVIOR} so it doesn't conflict with MOVE goals.
 */
public class PetAquaticMovementGoal implements Goal<Mob> {

    private static final double IDLE_SINK_VELOCITY = -0.005D;
    private static final double Y_FORCE_MULTIPLIER = 0.15D;
    private static final double MIN_Y_FORCE = 0.03D;
    private static final double RAD_TO_DEG = 57.2957763671875D;

    /**
     * Horizontal distance (blocks) at which the land push stops. Matches
     * {@code PetFollowOwnerGoal.STATIONARY_MAX_DIST_SQ} (9.0 = 3 blocks) so the pet
     * settles where the follow goal would rather than jittering against it.
     */
    private static final double LAND_STOP_DISTANCE = 3.0D;

    /** Base horizontal speed (blocks/tick) of the land push, before distance scaling. */
    private static final double LAND_BASE_SPEED = 0.10D;

    /**
     * Cap for the land push. A sprinting player covers ~0.13 blocks/tick, so this
     * leaves enough headroom to close a gap without outrunning the owner.
     */
    private static final double LAND_MAX_SPEED = 0.22D;

    /** Added to {@link #LAND_BASE_SPEED} per block of distance to the owner. */
    private static final double LAND_SPEED_PER_BLOCK = 0.01D;

    private final Pet pet;
    private final Mob mob;

    public PetAquaticMovementGoal(Pet pet, Mob mob) {
        this.pet = pet;
        this.mob = mob;
    }

    @Override
    public boolean shouldActivate() {
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
        if (!isInWaterOrBubble()) {
            tickOnLand();
            return;
        }

        Player owner = pet.getOwner().getPlayer();
        if (owner == null) return;

        Location petLoc = mob.getLocation();
        Location ownerLoc = owner.getLocation();
        float speed = (float) mob.getAttribute(PetAttributes.MOVEMENT_SPEED).getValue();

        boolean hasPath = mob.getPathfinder().hasPath();
        boolean hasTarget = pet.hasTarget() && pet.getPetTarget() != null;

        if (hasPath || hasTarget) {
            // Active swimming: apply Y velocity toward target
            Location targetLoc = hasTarget ? pet.getPetTarget().getLocation() : ownerLoc;
            double dy = targetLoc.getY() - petLoc.getY();
            double dx = targetLoc.getX() - petLoc.getX();
            double dz = targetLoc.getZ() - petLoc.getZ();
            double totalDist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (totalDist > 0.5) {
                // Y-axis velocity proportional to the Y component of direction
                double yRatio = dy / totalDist;
                double yVelocity = speed * yRatio * Y_FORCE_MULTIPLIER;

                // Ensure minimum velocity magnitude when Y difference is significant
                if (Math.abs(dy) > 1.0 && Math.abs(yVelocity) < MIN_Y_FORCE) {
                    yVelocity = dy > 0 ? MIN_Y_FORCE : -MIN_Y_FORCE;
                }

                // Direct-set Y velocity (do NOT add to existing). Adding caused
                // unbounded Y-axis accumulation over multiple ticks — water
                // friction (~0.8) wasn't strong enough to dampen the accumulated
                // value at the intended magnitude, producing vertical overshoot
                // and oscillation around the target depth. The idle branch
                // below correctly uses the same direct-set pattern.
                Vector vel = mob.getVelocity();
                mob.setVelocity(new Vector(vel.getX(), yVelocity, vel.getZ()));
            }

            // Body rotation to face swimming direction
            if (dx != 0.0D || dz != 0.0D) {
                float targetYaw = (float) (Math.atan2(dz, dx) * RAD_TO_DEG) - 90.0F;
                float newYaw = rotlerp(petLoc.getYaw(), targetYaw, 90.0F);
                mob.setRotation(newYaw, petLoc.getPitch());
                mob.setBodyYaw(newYaw);
            }
        } else {
            // Idle in water: gentle sinking
            Vector vel = mob.getVelocity();
            mob.setVelocity(new Vector(vel.getX(), IDLE_SINK_VELOCITY, vel.getZ()));
        }
    }

    /**
     * Pushes a stranded pet horizontally toward its owner, leaving the Y component —
     * and therefore the vanilla flop hop — alone. Re-applied every tick because ground
     * friction ({@code blockFriction * 0.91}, ~0.55 on most blocks) bleeds most of the
     * horizontal velocity away between ticks.
     */
    private void tickOnLand() {
        if (!pet.canMove()) {
            return; // sitting, or otherwise pinned — don't drag it around
        }
        if (!mob.isEmpty()) {
            return; // a rider steers it; setting velocity here would fight them
        }
        if (pet.getPetTarget() != null && !pet.getPetTarget().isDead()) {
            return; // target goals own the movement while there's something to fight
        }
        if (pet.getOwner() == null) return;
        Player owner = pet.getOwner().getPlayer();
        if (owner == null) return;

        Location petLoc = mob.getLocation();
        Location ownerLoc = owner.getLocation();
        // An owner who changed world is recovered by PlayerListener re-creating the pet
        // there; measuring distance across worlds would throw. Same guard as
        // PetFollowOwnerGoal.
        if (!petLoc.getWorld().equals(ownerLoc.getWorld())) return;

        double dx = ownerLoc.getX() - petLoc.getX();
        double dz = ownerLoc.getZ() - petLoc.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        if (horizontalDist < LAND_STOP_DISTANCE) {
            return; // close enough — let it flop in place
        }

        double speed = Math.min(LAND_MAX_SPEED, LAND_BASE_SPEED + horizontalDist * LAND_SPEED_PER_BLOCK);
        Vector vel = mob.getVelocity();
        mob.setVelocity(new Vector(dx / horizontalDist * speed, vel.getY(), dz / horizontalDist * speed));

        float targetYaw = (float) (Math.atan2(dz, dx) * RAD_TO_DEG) - 90.0F;
        float newYaw = rotlerp(petLoc.getYaw(), targetYaw, 90.0F);
        mob.setRotation(newYaw, petLoc.getPitch());
        mob.setBodyYaw(newYaw);
    }

    private boolean isInWaterOrBubble() {
        return mob.isInWater() || mob.isInBubbleColumn();
    }

    private static float rotlerp(float current, float target, float maxDelta) {
        float delta = wrapDegrees(target - current);
        if (delta > maxDelta) delta = maxDelta;
        if (delta < -maxDelta) delta = -maxDelta;
        return current + delta;
    }

    private static float wrapDegrees(float degrees) {
        degrees = degrees % 360.0F;
        if (degrees >= 180.0F) degrees -= 360.0F;
        if (degrees < -180.0F) degrees += 360.0F;
        return degrees;
    }

    @Override
    public @NotNull GoalKey<Mob> getKey() {
        return PetGoalKey.AQUATIC_MOVEMENT;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.UNKNOWN_BEHAVIOR);
    }
}

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
import de.Keyle.MyPet.entity.ai.PetGoalKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * Paper Goal that keeps a stranded aquatic pet moving toward its owner while it is
 * out of water. In water it does nothing — swimming is left to vanilla physics and
 * {@link PetFloatGoal}, exactly as before this goal existed.
 *
 * <p>It drives the pet horizontally toward its owner — or toward its target, when it has
 * one — on land, because a stranded water-breather has no movement of its own: vanilla's
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

    private static final double RAD_TO_DEG = 57.2957763671875D;

    /**
     * Horizontal distance (blocks) at which the land push toward the owner stops.
     * Matches {@code PetFollowOwnerGoal.STATIONARY_MAX_DIST_SQ} (9.0 = 3 blocks) so the
     * pet settles where the follow goal would rather than jittering against it.
     */
    private static final double LAND_STOP_DISTANCE = 3.0D;

    /**
     * Horizontal distance at which the push toward a <em>target</em> stops. Tighter than
     * {@link #LAND_STOP_DISTANCE} because it has to land inside
     * {@code PetMeleeAttackGoal}'s reach ({@code mob.getWidth() + 1.3} plus two-thirds of
     * the target's height — a shade over 3 blocks for a fish-sized attacker against a
     * player-sized target). Stopping at 3.0 would park the pet right on the edge of that
     * reach and let it drift out again.
     */
    private static final double TARGET_STOP_DISTANCE = 2.0D;

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
        if (isInWaterOrBubble()) {
            // Swimming is not this goal's business. PetFloatGoal already writes Y every
            // tick in water, and vanilla's GoalSelector ticks goals in insertion order
            // (an ObjectLinkedOpenHashSet — priority only arbitrates flag conflicts), so
            // anything written here would silently override it. Notably PetFloatGoal
            // bails on !canMove(), which is the only thing holding a sitting aquatic pet
            // in place; a Y write here would sink it.
            return;
        }
        tickOnLand();
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
        Location petLoc = mob.getLocation();
        // A live target outranks the owner. This goal cannot delegate to the target goals
        // the way a land pet does: PetMeleeAttackGoal steers through the navigation, and
        // a water-bound navigation is inert on land, so the velocity push here is the
        // pet's ONLY way to close on something it has decided to fight. Bailing out on
        // `hasTarget` (as this did originally) left a stranded pet frozen the moment it
        // picked a target, with the follow goal also switched off for the same reason.
        LivingEntity target = pet.getPetTarget();
        boolean chasing = target != null && !target.isDead()
                && Bukkit.isOwnedByCurrentRegion(target)
                && target.getWorld().equals(petLoc.getWorld());

        Location destination;
        double stopDistance;
        if (chasing) {
            destination = target.getLocation();
            stopDistance = TARGET_STOP_DISTANCE;
        } else {
            if (pet.getOwner() == null) return;
            Player owner = pet.getOwner().getPlayer();
            if (owner == null) return;
            destination = owner.getLocation();
            stopDistance = LAND_STOP_DISTANCE;
            // An owner who changed world is recovered by PlayerListener re-creating the
            // pet there; measuring distance across worlds would throw. Same guard as
            // PetFollowOwnerGoal.
            if (!petLoc.getWorld().equals(destination.getWorld())) return;
        }

        double dx = destination.getX() - petLoc.getX();
        double dz = destination.getZ() - petLoc.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        if (horizontalDist < stopDistance) {
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

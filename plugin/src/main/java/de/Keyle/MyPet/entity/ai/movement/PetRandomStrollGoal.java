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
import org.bukkit.Bukkit;
import org.bukkit.entity.Mob;
import de.Keyle.MyPet.entity.ai.PetGoalKey;
import de.Keyle.MyPet.entity.ai.PetGoalWorlds;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Paper {@link Goal} that makes a ground-based pet wander within a small
 * radius of its owner when the owner is standing still. Uses a simple
 * "random offset + block validation" routine driven through Paper's
 * {@link com.destroystokyo.paper.entity.Pathfinder}.
 *
 * <p>The goal deliberately only runs while the <em>owner</em> is
 * stationary — measured by an EMA of the owner's horizontal movement over
 * recent ticks, compared against {@link #OWNER_STATIONARY_THRESHOLD}. A pet
 * that's already catching up to a walking owner via
 * {@code PetFollowOwnerGoal} shouldn't also pick its own stroll target and
 * tug the navigation in two directions, so the two goals are
 * self-sequencing via the owner-stationary gate rather than through
 * mutual {@link GoalType} exclusion.
 *
 * <p>Stroll target selection tries up to {@link #MAX_ATTEMPTS} random
 * offsets within {@link #WANDER_RADIUS} blocks of the owner; each
 * candidate is raycast down to the highest block and validated to stand
 * on solid ground with an air column at the pet's feet. Bounding the
 * candidate to the owner's neighbourhood (rather than also filtering by
 * pet-to-candidate distance) keeps strolling responsive when the pet is
 * farther than a couple of blocks away.
 *
 * <p>When {@link PetControlGoal} is driving the pet toward an owner-chosen
 * location, strolling must step aside. The control goal is resolved lazily
 * via {@code Bukkit.getMobGoals().getGoal()} on the first
 * {@link #shouldActivate()} call and cached; a {@link #controlGoalLookupDone
 * sentinel flag} skips the lookup on pets that have no control goal (e.g.
 * flying pets) so the per-tick path doesn't iterate the mob's goal list
 * forever.
 *
 * <p>{@code protected} fields and helpers allow specialized subclasses
 * (fly/swim variants) to reuse the owner-stationary EMA logic.
 */
public class PetRandomStrollGoal implements Goal<Mob> {

    private static final double STATIONARY_MAX_DIST_SQ = 9.0;
    private static final float DEFAULT_STROLL_CHANCE = 0.005F;
    private static final double OWNER_STATIONARY_THRESHOLD = 0.03;
    private static final double SPEED_SMOOTHING = 0.2;
    private static final int WANDER_RADIUS = 2;
    private static final double STROLL_SPEED = 0.15;
    private static final int MAX_ATTEMPTS = 5;
    private static final double DESTINATION_REACHED_SQ = 0.5625; // 0.75²

    protected final Pet pet;
    protected final Mob mob;
    protected Location moveTo = null;
    protected int timeToMove = 0;
    protected float strollChance = DEFAULT_STROLL_CHANCE;
    protected boolean ownerStationary = false;
    protected double ownerMovementSpeed = 0;
    private double lastOwnerX, lastOwnerZ;
    private boolean ownerPositionInitialized = false;
    private PetControlGoal controlGoal;
    /**
     * Sentinel for the controlGoal lookup. Flying pets have no
     * PetControlGoal registered, so the {@code getMobGoals().getGoal()}
     * lookup returns null on every tick. Set this after the first lookup to
     * skip subsequent calls and avoid per-tick goal-list iteration.
     */
    private boolean controlGoalLookupDone = false;

    /**
     * @param petEntity the pet that will wander when its owner stands still
     */
    public PetRandomStrollGoal(Pet pet, Mob mob) {
        this.pet = pet;
        this.mob = mob;
    }

    protected void updateOwnerMovement(Player owner) {
        double ownerX = owner.getLocation().getX();
        double ownerZ = owner.getLocation().getZ();
        if (!ownerPositionInitialized) {
            lastOwnerX = ownerX;
            lastOwnerZ = ownerZ;
            ownerPositionInitialized = true;
            ownerStationary = true;
            return;
        }
        double dx = ownerX - lastOwnerX;
        double dz = ownerZ - lastOwnerZ;
        double current = Math.sqrt(dx * dx + dz * dz);
        ownerMovementSpeed = SPEED_SMOOTHING * current + (1.0 - SPEED_SMOOTHING) * ownerMovementSpeed;
        lastOwnerX = ownerX;
        lastOwnerZ = ownerZ;
        ownerStationary = ownerMovementSpeed < OWNER_STATIONARY_THRESHOLD;
    }

    @Override
    public boolean shouldActivate() {
        if (!Bukkit.isOwnedByCurrentRegion(mob)) {
            return false;
        }
        if (ThreadLocalRandom.current().nextFloat() >= strollChance) {
            return false;
        }
        if (controlGoal == null && !controlGoalLookupDone) {
            // petEntity may be a LegacyBukkitAdapter proxy — use the real Bukkit Mob
            // for Paper's internal CraftMob-keyed lookup.
            var goal = Bukkit.getMobGoals().getGoal(pet.getBukkitEntity(), PetGoalKey.CONTROL);
            if (goal instanceof PetControlGoal pcg) controlGoal = pcg;
            controlGoalLookupDone = true;
        }
        if (!pet.canMove()) return false;
        if (pet.hasTarget() && !pet.getPetTarget().isDead()) return false;

        Player owner = pet.getOwner().getPlayer();
        if (owner == null) return false;
        // Strolling is anchored to the owner; measuring distance to one in another
        // world would throw.
        if (PetGoalWorlds.isCrossWorld(mob, owner)) return false;

        updateOwnerMovement(owner);
        if (!ownerStationary) return false;

        double distSq = mob.getLocation().distanceSquared(owner.getLocation());
        if (distSq > STATIONARY_MAX_DIST_SQ) return false;

        return controlGoal == null || controlGoal.moveTo == null;
    }

    @Override
    public boolean shouldStayActive() {
        if (!Bukkit.isOwnedByCurrentRegion(mob)) return false;
        if (controlGoal != null && controlGoal.moveTo != null) return false;
        Player owner = pet.getOwner().getPlayer();
        if (owner == null) return false;
        if (PetGoalWorlds.isCrossWorld(mob, owner)) return false;
        updateOwnerMovement(owner);
        if (!ownerStationary && mob.getLocation().distanceSquared(owner.getLocation()) > STATIONARY_MAX_DIST_SQ)
            return false;
        if (!pet.canMove()) return false;
        if (moveTo == null) return false;
        // moveTo was picked in the owner's world; the pet may have left it since.
        if (PetGoalWorlds.isCrossWorld(mob, moveTo)) return false;
        if (mob.getLocation().distanceSquared(moveTo) < DESTINATION_REACHED_SQ) return false;
        if (timeToMove <= 0) return false;
        if (pet.hasTarget() && !pet.getPetTarget().isDead()) return false;
        return true;
    }

    @Override
    public void start() {
        if (!Bukkit.isOwnedByCurrentRegion(mob)) return;
        Location target = findStrollTarget();
        if (target == null) return;
        moveTo = target;
        timeToMove = Math.max(3, (int) (mob.getLocation().distance(moveTo) / 3));
        // Add the speed modifier only after navigation is accepted, so a
        // rejected path doesn't leave a live "RandomStroll" modifier that
        // stop() will only clean up on some later tick.
        if (pet.getPetNavigation().navigateTo(moveTo)) {
            pet.getPetNavigation().getParameters().addSpeedModifier("RandomStroll", STROLL_SPEED);
        } else {
            moveTo = null;
        }
    }

    @Override
    public void stop() {
        pet.getPetNavigation().getParameters().removeSpeedModifier("RandomStroll");
        pet.getPetNavigation().stop();
        moveTo = null;
    }

    @Override
    public void tick() {
        timeToMove--;
    }

    protected Location findStrollTarget() {
        Player owner = pet.getOwner().getPlayer();
        if (owner == null) return null;
        Location ownerLoc = owner.getLocation();
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            double dx = rng.nextDouble(-WANDER_RADIUS, WANDER_RADIUS + 1);
            double dz = rng.nextDouble(-WANDER_RADIUS, WANDER_RADIUS + 1);
            Location candidate = ownerLoc.clone().add(dx, 0, dz);
            // Find ground level
            Block block = candidate.getWorld().getHighestBlockAt(candidate);
            candidate.setY(block.getY() + 1);

            // Validate: solid ground below, air at feet
            Block below = candidate.getBlock().getRelative(BlockFace.DOWN);
            if (!below.getType().isSolid()) continue;
            if (candidate.getBlock().getType().isSolid()) continue;

            // No explicit pet-to-candidate distance filter: the outer
            // STATIONARY_MAX_DIST_SQ gate in shouldActivate() already keeps
            // the pet within ~3 blocks of the owner, and candidates are
            // generated within WANDER_RADIUS of the owner, so the
            // pet-to-candidate distance is bounded implicitly.

            return candidate;
        }
        return null;
    }

    @Override
    public @NotNull GoalKey<Mob> getKey() {
        return PetGoalKey.RANDOM_STROLL;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE);
    }
}

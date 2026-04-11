package de.Keyle.MyPet.entity.ai.movement;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import de.Keyle.MyPet.api.entity.MyPet;
import org.bukkit.entity.Mob;
import de.Keyle.MyPet.entity.ai.PetGoalKey;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Paper {@link Goal} that lets an aquatic pet drift around its stationary
 * owner while submerged. The water analogue of
 * {@link PetRandomStrollGoal} / {@link PetRandomFlyGoal}, with candidate
 * validation restricted to water blocks.
 *
 * <p>Unlike the ground/air variants, this goal first checks that the pet
 * is actually in water before sampling a candidate position — on dry land
 * the swim goal has nothing to do. Candidates are drawn from a 3-D box
 * around the owner and accepted only when they land inside
 * {@link Material#WATER} or {@link Material#BUBBLE_COLUMN}, and they must
 * be within ~3 blocks of the pet itself.
 *
 * <p>Like its siblings the activation chance is rolled per tick; the swim
 * chance is considerably higher than the stroll / fly chance because
 * aquatic pets visibly "drift" far more than land pets — the higher roll
 * keeps the idle motion feeling natural.
 *
 * <p>The speed modifier applied during navigation is keyed
 * {@code "RandomStroll"} to match {@link PetRandomStrollGoal} and
 * {@link PetRandomFlyGoal}, so swapping goal instances during a pet's
 * lifecycle doesn't leave an orphan modifier under a stale key.
 */
public class PetRandomSwimGoal implements Goal<Mob> {

    private static final double STATIONARY_MAX_DIST_SQ = 9.0;
    private static final float SWIM_STROLL_CHANCE = 0.06F;
    private static final double OWNER_STATIONARY_THRESHOLD = 0.03;
    private static final double SPEED_SMOOTHING = 0.2;
    private static final int WANDER_RADIUS_H = 3;
    private static final int WANDER_RADIUS_V = 2;
    private static final double SWIM_SPEED = 0.25;
    private static final int MAX_ATTEMPTS = 5;

    private final MyPet pet;
    private final Mob mob;
    private Location moveTo = null;
    private int timeToMove = 0;
    private boolean ownerStationary = false;
    private double ownerMovementSpeed = 0;
    private double lastOwnerX, lastOwnerZ;
    private boolean ownerPositionInitialized = false;

    /**
     * @param petEntity the aquatic pet that will drift when its owner stands still
     */
    public PetRandomSwimGoal(MyPet pet, Mob mob) {
        this.pet = pet;
        this.mob = mob;
    }

    private void updateOwnerMovement(Player owner) {
        double ownerX = owner.getLocation().getX();
        double ownerZ = owner.getLocation().getZ();
        // First-call guard: on the very first tick after the goal is
        // constructed, lastOwnerX/Z are still at their zero defaults, so the
        // naive subtraction below would compute the owner's *distance from
        // world origin* instead of their per-tick movement, poisoning the
        // EMA with a huge spurious value. Snapshot the current position and
        // bail before the delta calculation. Same pattern as
        // PetRandomStrollGoal and PetRandomFlyGoal.
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
        if (ThreadLocalRandom.current().nextFloat() >= SWIM_STROLL_CHANCE) return false;
        if (!pet.canMove()) return false;
        if (pet.hasTarget() && !pet.getMyPetTarget().isDead()) return false;

        Player owner = pet.getOwner().getPlayer();
        if (owner == null) return false;

        updateOwnerMovement(owner);
        if (!ownerStationary) return false;

        return mob.getLocation().distanceSquared(owner.getLocation()) <= STATIONARY_MAX_DIST_SQ;
    }

    @Override
    public boolean shouldStayActive() {
        Player owner = pet.getOwner().getPlayer();
        if (owner == null) return false;
        updateOwnerMovement(owner);
        if (!pet.canMove()) return false;
        if (moveTo == null) return false;
        if (mob.getLocation().distance(moveTo) < 0.75) return false;
        if (timeToMove <= 0) return false;
        if (pet.hasTarget() && !pet.getMyPetTarget().isDead()) return false;
        return true;
    }

    @Override
    public void start() {
        Location target = findSwimTarget();
        if (target == null) return;
        moveTo = target;
        timeToMove = Math.max(3, (int) (mob.getLocation().distance(moveTo) / 3));
        // See PetRandomStrollGoal.start(): gate the speed modifier on a
        // successful navigateTo() to avoid leaking it on path rejection.
        if (pet.getPetNavigation().navigateTo(moveTo)) {
            pet.getPetNavigation().getParameters().addSpeedModifier("RandomStroll", SWIM_SPEED);
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

    private Location findSwimTarget() {
        Player owner = pet.getOwner().getPlayer();
        if (owner == null) return null;

        // If not in water, don't swim-stroll
        if (!mob.isInWater()) return null;

        Location ownerLoc = owner.getLocation();
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            double dx = rng.nextDouble(-WANDER_RADIUS_H, WANDER_RADIUS_H + 1);
            double dy = rng.nextDouble(-WANDER_RADIUS_V, WANDER_RADIUS_V + 1);
            double dz = rng.nextDouble(-WANDER_RADIUS_H, WANDER_RADIUS_H + 1);
            Location candidate = ownerLoc.clone().add(dx, dy, dz);

            // Validate: must be water
            Material type = candidate.getBlock().getType();
            if (type != Material.WATER && type != Material.BUBBLE_COLUMN) continue;

            // Check distance from pet
            if (mob.getLocation().distanceSquared(candidate) > 9.0) continue;

            return candidate;
        }
        return null;
    }

    @Override
    public @NotNull GoalKey<Mob> getKey() {
        return PetGoalKey.RANDOM_SWIM;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE);
    }
}

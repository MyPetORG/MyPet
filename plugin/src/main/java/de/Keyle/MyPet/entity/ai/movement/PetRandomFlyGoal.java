package de.Keyle.MyPet.entity.ai.movement;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import de.Keyle.MyPet.api.entity.MyPet;
import org.bukkit.entity.Mob;
import de.Keyle.MyPet.entity.ai.PetGoalKey;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;


/**
 * Paper {@link Goal} that lets an aerial pet wander in a small 3-D volume
 * above its stationary owner. The flying analogue of
 * {@link PetRandomStrollGoal}, using axis-aligned offset sampling to pick
 * candidate hover positions.
 *
 * <p>Like the ground variant, activation is gated on the owner being
 * stationary — measured by an EMA of the owner's horizontal movement vs
 * {@link #OWNER_STATIONARY_THRESHOLD} — so the pet never fights the
 * follow-owner goal for navigation control. Candidate positions are
 * sampled in an H×V×H box of size
 * {@code (2*WANDER_RADIUS_H+1) × (2*WANDER_RADIUS_V+1) × (2*WANDER_RADIUS_H+1)}
 * around the owner and shifted up by two blocks so the pet hovers above
 * rather than landing on top of the owner.
 *
 * <p>The speed modifier applied during navigation is deliberately keyed
 * {@code "RandomStroll"} (the same key used by {@link PetRandomStrollGoal})
 * so swapping goal instances during a pet's lifecycle doesn't leave an
 * orphan modifier under a stale key.
 */
public class PetRandomFlyGoal implements Goal<Mob> {

    private static final double STATIONARY_MAX_DIST_SQ = 9.0;
    private static final float FLY_STROLL_CHANCE = 0.005F;
    private static final double OWNER_STATIONARY_THRESHOLD = 0.03;
    private static final double SPEED_SMOOTHING = 0.2;
    private static final int WANDER_RADIUS_H = 2;
    private static final int WANDER_RADIUS_V = 2;
    private static final double FLY_SPEED = 0.2;
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
     * @param petEntity the aerial pet that will hover-wander when its owner stands still
     */
    public PetRandomFlyGoal(MyPet pet, Mob mob) {
        this.pet = pet;
        this.mob = mob;
    }

    private void updateOwnerMovement(Player owner) {
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
        if (ThreadLocalRandom.current().nextFloat() >= FLY_STROLL_CHANCE) return false;
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
        Location target = findFlyTarget();
        if (target == null) return;
        moveTo = target;
        timeToMove = Math.max(3, (int) (mob.getLocation().distance(moveTo) / 3));
        // See PetRandomStrollGoal.start(): gate the speed modifier on a
        // successful navigateTo() to avoid leaking it on path rejection.
        if (pet.getPetNavigation().navigateTo(moveTo)) {
            pet.getPetNavigation().getParameters().addSpeedModifier("RandomStroll", FLY_SPEED);
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

    private Location findFlyTarget() {
        Player owner = pet.getOwner().getPlayer();
        if (owner == null) return null;
        Location ownerLoc = owner.getLocation();
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            double dx = rng.nextDouble(-WANDER_RADIUS_H, WANDER_RADIUS_H + 1);
            double dy = rng.nextDouble(-WANDER_RADIUS_V, WANDER_RADIUS_V + 1);
            double dz = rng.nextDouble(-WANDER_RADIUS_H, WANDER_RADIUS_H + 1);
            Location candidate = ownerLoc.clone().add(dx, dy + 2, dz); // +2 to hover above owner

            // Validate: not inside a solid block
            if (candidate.getBlock().getType().isSolid()) continue;

            // No explicit pet-to-candidate distance filter: the outer
            // STATIONARY_MAX_DIST_SQ gate in shouldActivate() keeps the pet
            // within ~3 blocks of the owner, and candidates are generated
            // within WANDER_RADIUS_H / WANDER_RADIUS_V of the owner (plus a
            // +2 hover offset), bounding the pet-to-candidate distance
            // implicitly. The old `> 9.0` filter (dist > 3 blocks) rejected
            // almost every candidate given the 3D reach of the generator.

            return candidate;
        }
        return null;
    }

    @Override
    public @NotNull GoalKey<Mob> getKey() {
        return PetGoalKey.RANDOM_FLY;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE);
    }
}

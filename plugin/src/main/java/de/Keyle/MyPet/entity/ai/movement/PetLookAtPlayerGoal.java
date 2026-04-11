package de.Keyle.MyPet.entity.ai.movement;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import de.Keyle.MyPet.api.entity.MyPet;
import org.bukkit.entity.Mob;
import de.Keyle.MyPet.entity.ai.PetGoalKey;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Paper {@link Goal} replacement for vanilla's {@code LookAtPlayerGoal} —
 * periodically turns the pet's head to look at the nearest {@link Player}.
 *
 * <p>Activation is a combination of a low random chance
 * ({@code lookAtPlayerChance}, default {@code 0.02}) and three hard
 * requirements: the pet must not be combat-targeted (so it doesn't
 * distraction-glance mid-fight), must not have any passengers (riders want
 * head stability), and a player must exist within {@code range} blocks.
 *
 * <p>Once selected, the look persists for 40–79 ticks, after which
 * {@link #shouldStayActive()} returns {@code false} and the head rotation
 * falls back to whatever the next-best LOOK goal produces (typically
 * {@link PetRandomLookaroundGoal}).
 *
 * <p>The tick loop guards against a known goal-preemption race where
 * Paper's selector can interleave a freshly-called {@link #stop()} (which
 * nulls {@code targetPlayer}) with a still-pending {@link #tick()} from the
 * same activation cycle; the cheap null-check on {@code targetPlayer}
 * avoids an NPE in that window.
 *
 * <p>Declares {@link GoalType#LOOK} so it competes only with other LOOK
 * goals and leaves MOVE and TARGET goals free to run concurrently.
 */
public class PetLookAtPlayerGoal implements Goal<Mob> {

    private final MyPet pet;
    private final Mob mob;
    private final double range;
    private final float lookAtPlayerChance;
    private Player targetPlayer;
    private int ticksUntilStopLooking;

    /**
     * Convenience constructor defaulting the per-tick activation roll to {@code 0.02}
     * (matches vanilla's {@code LookAtPlayerGoal} frequency).
     *
     * @param petEntity the pet whose head will rotate
     * @param range     maximum look distance in blocks
     */
    public PetLookAtPlayerGoal(MyPet pet, Mob mob, float range) {
        this(pet, mob, range, 0.02F);
    }

    /**
     * @param petEntity          the pet whose head will rotate
     * @param range              maximum look distance in blocks
     * @param lookAtPlayerChance per-tick probability of activation ({@code 0}..{@code 1});
     *                           only rolled when an eligible player is in range
     */
    public PetLookAtPlayerGoal(MyPet pet, Mob mob, float range, float lookAtPlayerChance) {
        this.pet = pet;
        this.mob = mob;
        this.range = range;
        this.lookAtPlayerChance = lookAtPlayerChance;
    }

    @Override
    public boolean shouldActivate() {
        if (ThreadLocalRandom.current().nextFloat() >= this.lookAtPlayerChance) {
            return false;
        }
        if (pet.hasTarget() && !pet.getMyPetTarget().isDead()) {
            return false;
        }
        if (!mob.getPassengers().isEmpty()) {
            return false;
        }
        Location loc = mob.getLocation();
        Collection<Entity> nearby = loc.getWorld().getNearbyEntities(loc, range, range, range,
                e -> e instanceof Player && !e.equals(mob));
        Player nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        for (Entity e : nearby) {
            double distSq = e.getLocation().distanceSquared(loc);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = (Player) e;
            }
        }
        this.targetPlayer = nearest;
        return this.targetPlayer != null;
    }

    @Override
    public boolean shouldStayActive() {
        if (targetPlayer == null || targetPlayer.isDead()) {
            return false;
        }
        if (mob.getLocation().distanceSquared(targetPlayer.getLocation()) > range * range) {
            return false;
        }
        if (!mob.getPassengers().isEmpty()) {
            return false;
        }
        return this.ticksUntilStopLooking > 0;
    }

    @Override
    public void start() {
        this.ticksUntilStopLooking = 40 + ThreadLocalRandom.current().nextInt(40);
    }

    @Override
    public void stop() {
        this.targetPlayer = null;
    }

    @Override
    public void tick() {
        // Guard against goal preemption races: stop() nulls targetPlayer, and
        // Paper's goal selector can in principle interleave a preempted
        // stop() with a still-pending tick() from the same activation cycle.
        // Cheap null-check here is the safe default.
        if (targetPlayer == null) {
            return;
        }
        mob.lookAt(targetPlayer, mob.getHeadRotationSpeed(), mob.getMaxHeadPitch());
        this.ticksUntilStopLooking--;
    }

    @Override
    public @NotNull GoalKey<Mob> getKey() {
        return PetGoalKey.LOOK_AT_PLAYER;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.LOOK);
    }
}

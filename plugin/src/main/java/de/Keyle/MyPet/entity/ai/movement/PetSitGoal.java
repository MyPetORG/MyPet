package de.Keyle.MyPet.entity.ai.movement;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.entity.ai.PetGoalKey;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;

/**
 * Paper {@link Goal} that holds a sit-capable pet seated until its owner
 * explicitly toggles sitting off.
 *
 * <p>The {@link #SITTABLE_TYPES} allowlist enforces that only the vanilla
 * pet species that actually have a sit animation (wolf, cat, camel, panda,
 * fox) can enter the goal — other pets' sit commands fall through to
 * nothing rather than showing a broken pose.
 *
 * <p>Unlike most goals, {@link #shouldStayActive()} does <em>not</em>
 * re-run {@link #shouldActivate()}. A default "re-check every tick"
 * implementation would cancel the sit the instant the pet was bumped
 * off the ground or splashed by water for a single tick; instead this
 * implementation holds the sit until the owner toggles it off.
 *
 * <p>The goal declares all three core {@link GoalType GoalTypes}
 * ({@link GoalType#MOVE MOVE}, {@link GoalType#LOOK LOOK},
 * {@link GoalType#JUMP JUMP}) so while sitting the pet is locked out of
 * every other motion goal — no strolling, no head-turning, no jumping.
 *
 * <p>{@link #isSitting()} / {@link #setSitting(boolean)} /
 * {@link #toggleSitting()} form the public handle used by the sit
 * command's executor; they simply flip the {@code sitting} flag and let
 * the goal selector pick the goal up (or drop it) on the next tick.
 */
public class PetSitGoal implements Goal<Mob> {

    private static final Set<String> SITTABLE_TYPES = Set.of("Wolf", "Cat", "Camel", "Panda", "Fox");

    private final MyPetBukkitEntity petEntity;
    private boolean sitting = false;

    /**
     * @param petEntity the pet that will be commanded to sit
     */
    public PetSitGoal(MyPetBukkitEntity petEntity) {
        this.petEntity = petEntity;
    }

    @Override
    public boolean shouldActivate() {
        if (!SITTABLE_TYPES.contains(petEntity.getPetType().name())) {
            return false;
        }
        if (petEntity.isInWater()) {
            return false;
        }
        if (!petEntity.isOnGround()) {
            return false;
        }
        return this.sitting;
    }

    @Override
    public boolean shouldStayActive() {
        // Deliberately does NOT re-run shouldActivate(): a one-tick bump
        // off the ground or water splash would otherwise cancel the sit.
        // Hold it until the owner explicitly toggles sitting off.
        return this.sitting;
    }

    @Override
    public void start() {
        petEntity.getPathfinder().stopPathfinding();
        petEntity.setSitting(true);
        petEntity.setTarget(null);
    }

    @Override
    public void stop() {
        petEntity.setSitting(false);
    }

    @Override
    public @NotNull GoalKey<Mob> getKey() {
        return PetGoalKey.SIT;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE, GoalType.LOOK, GoalType.JUMP);
    }

    /** @return {@code true} if the pet is currently commanded to sit */
    public boolean isSitting() {
        return this.sitting;
    }

    /**
     * Commands the pet to sit or stand. Paper's goal selector picks up
     * the change on its next tick — this method does not itself start
     * or stop the goal.
     */
    public void setSitting(boolean sitting) {
        this.sitting = sitting;
    }

    /** Flips the sit state, exactly as the {@code /petsit} command toggles it. */
    public void toggleSitting() {
        this.sitting = !this.sitting;
    }
}

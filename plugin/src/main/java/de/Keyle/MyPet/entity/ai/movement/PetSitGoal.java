package de.Keyle.MyPet.entity.ai.movement;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import de.Keyle.MyPet.api.entity.MyPet;
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
 * <p>This goal is a pure <em>view</em> over {@link MyPet#isSitting()} —
 * it owns no sitting state of its own. The interact handler in
 * {@code MyPet#onInteract} flips {@code MyPet.sitting} and the goal
 * selector picks the change up on its next tick.
 */
public class PetSitGoal implements Goal<Mob> {

    private static final Set<String> SITTABLE_TYPES = Set.of("Wolf", "Cat", "Camel", "Panda", "Fox");

    private final MyPet pet;
    private final Mob mob;

    /**
     * @param petEntity the pet that will be commanded to sit
     */
    public PetSitGoal(MyPet pet, Mob mob) {
        this.pet = pet;
        this.mob = mob;
    }

    @Override
    public boolean shouldActivate() {
        if (!SITTABLE_TYPES.contains(pet.getPetType().name())) {
            return false;
        }
        if (mob.isInWater()) {
            return false;
        }
        if (!mob.isOnGround()) {
            return false;
        }
        return pet.isSitting();
    }

    @Override
    public boolean shouldStayActive() {
        // Deliberately does NOT re-run shouldActivate(): a one-tick bump
        // off the ground or water splash would otherwise cancel the sit.
        // Hold it until the owner explicitly toggles sitting off.
        return pet.isSitting();
    }

    @Override
    public void start() {
        mob.getPathfinder().stopPathfinding();
        pet.setTarget(null);
    }

    @Override
    public void stop() {
    }

    @Override
    public @NotNull GoalKey<Mob> getKey() {
        return PetGoalKey.SIT;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE, GoalType.LOOK, GoalType.JUMP);
    }
}

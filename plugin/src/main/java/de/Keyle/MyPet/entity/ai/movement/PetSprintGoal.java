package de.Keyle.MyPet.entity.ai.movement;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.entity.ai.PetGoalKey;
import de.Keyle.MyPet.skill.skills.SprintImpl;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * Paper {@link Goal} driving the {@code Sprint} skill's speed boost —
 * when the pet picks up a brand-new melee target it applies a temporary
 * navigation speed modifier so the chase looks urgent.
 *
 * <p>Activation conditions:
 * <ul>
 *   <li>The {@link SprintImpl Sprint skill} must be active.</li>
 *   <li>The pet must have non-zero melee damage — sprint is a melee
 *       chase booster, not a ranged skill.</li>
 *   <li>The current target must be different from {@code lastTarget};
 *       sprint is a <em>new-target burst</em>, not a sustained effect.</li>
 *   <li>If the pet also has ranged damage, the target must be within
 *       16 m² (4 blocks); farther than that, ranged takes over and
 *       sprinting would waste the boost.</li>
 * </ul>
 *
 * <p>The goal stays active until the target dies, crosses worlds, or
 * closes the 4-block gap — at which point the sprint "catches up" and the
 * speed modifier is removed so melee combat runs at normal speed again.
 *
 * <p>Declares no {@link GoalType GoalTypes} so it runs concurrently with
 * whatever movement or target goal is driving the chase; it only applies
 * a side-effect (the {@code "Sprint"}-keyed speed modifier) rather than
 * taking over navigation itself.
 */
public class PetSprintGoal implements Goal<Mob> {

    private final MyPetBukkitEntity petEntity;
    private final MyPet myPet;
    private final float walkSpeedModifier;
    private LivingEntity lastTarget = null;

    /**
     * @param petEntity         the pet whose chase should get the sprint boost
     * @param walkSpeedModifier multiplicative navigation speed modifier applied while sprinting
     */
    public PetSprintGoal(MyPetBukkitEntity petEntity, float walkSpeedModifier) {
        this.petEntity = petEntity;
        this.myPet = petEntity.getMyPet();
        this.walkSpeedModifier = walkSpeedModifier;
    }

    @Override
    public boolean shouldActivate() {
        if (!myPet.getSkills().isActive(SprintImpl.class)) {
            return false;
        }
        if (myPet.getDamage() <= 0) {
            return false;
        }
        if (!petEntity.hasTarget()) {
            return false;
        }
        LivingEntity target = petEntity.getMyPetTarget();
        if (target == null || target.isDead()) {
            return false;
        }
        if (target.equals(lastTarget)) {
            return false;
        }
        if (myPet.getRangedDamage() > 0 && petEntity.getLocation().distanceSquared(target.getLocation()) >= 16) {
            return false;
        }
        this.lastTarget = target;
        return true;
    }

    @Override
    public boolean shouldStayActive() {
        if (petEntity.getOwner() == null) {
            return false;
        }
        if (lastTarget == null || lastTarget.isDead()) {
            return false;
        }
        if (petEntity.getLocation().distanceSquared(lastTarget.getLocation()) < 16) {
            return false;
        }
        return petEntity.canMove();
    }

    @Override
    public void start() {
        petEntity.getHandle().getPetNavigation().getParameters().addSpeedModifier("Sprint", walkSpeedModifier);
    }

    @Override
    public void stop() {
        petEntity.getHandle().getPetNavigation().getParameters().removeSpeedModifier("Sprint");
    }

    @Override
    public @NotNull GoalKey<Mob> getKey() {
        return PetGoalKey.SPRINT;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.noneOf(GoalType.class);
    }
}

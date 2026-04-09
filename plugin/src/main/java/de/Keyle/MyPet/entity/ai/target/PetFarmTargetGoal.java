package de.Keyle.MyPet.entity.ai.target;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.entity.ai.target.TargetPriority;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.api.skill.skills.Behavior.BehaviorMode;
import de.Keyle.MyPet.entity.ai.PetGoalKey;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * Paper {@link Goal} that auto-targets hostile {@link Monster Monsters}
 * near the owner when the pet's {@link Behavior} skill is set to
 * {@link BehaviorMode#Farm}. The "farm" mode is the XP-grinding companion
 * to {@link BehaviorMode#Aggressive}: the pet only picks fights with
 * genuine hostile mobs and leaves passive animals alone, so an owner can
 * stand inside a mob farm and harvest drops without the pet tripping on
 * cows and chickens.
 *
 * <p>Candidate filtering is intentionally minimal: any non-dead
 * {@link Monster} within ~9.5 blocks (91 m²) of the pet that passes the
 * hook helper's {@code canHurt} check is accepted. Unlike
 * {@link PetAggressiveTargetGoal}, there's no ownership filter because
 * {@link Monster} excludes tamed/owned entities by definition.
 *
 * <p>Declares {@link GoalType#TARGET}, making it mutually exclusive with
 * other target-acquisition goals.
 */
public class PetFarmTargetGoal implements Goal<Mob> {

    private final MyPetBukkitEntity petEntity;
    private final MyPet myPet;
    private final double range;
    private LivingEntity target;

    /**
     * @param petEntity the pet that will acquire monster targets while in Farm mode
     * @param range     radius (in blocks) of the "near owner" search box
     */
    public PetFarmTargetGoal(MyPetBukkitEntity petEntity, float range) {
        this.petEntity = petEntity;
        this.myPet = petEntity.getMyPet();
        this.range = range;
    }

    @Override
    public boolean shouldActivate() {
        Behavior behaviorSkill = myPet.getSkills().get(Behavior.class);
        if (!behaviorSkill.isActive() || behaviorSkill.getBehavior() != BehaviorMode.Farm) {
            return false;
        }
        if (myPet.getDamage() <= 0 && myPet.getRangedDamage() <= 0) {
            return false;
        }
        if (!petEntity.canMove()) {
            return false;
        }
        if (petEntity.hasTarget()) {
            return false;
        }

        Player owner = petEntity.getOwner().getPlayer();
        if (owner == null) {
            return false;
        }
        Location ownerLoc = owner.getLocation();
        Location petLoc = petEntity.getLocation();

        for (Entity entity : ownerLoc.getWorld().getNearbyEntities(ownerLoc, range, range, range)) {
            if (!(entity instanceof Monster monster)) {
                continue;
            }
            if (monster.isDead()) {
                continue;
            }
            if (petLoc.distanceSquared(monster.getLocation()) > 91) {
                continue;
            }
            if (!MyPetApi.getHookHelper().canHurt(owner, monster)) {
                continue;
            }
            this.target = monster;
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldStayActive() {
        if (!petEntity.canMove()) {
            return false;
        }
        if (!petEntity.hasTarget()) {
            return false;
        }
        LivingEntity currentTarget = petEntity.getMyPetTarget();
        if (currentTarget == null || currentTarget.isDead()) {
            return false;
        }
        Behavior behaviorSkill = myPet.getSkills().get(Behavior.class);
        if (behaviorSkill.getBehavior() != BehaviorMode.Farm) {
            return false;
        }
        if (myPet.getDamage() <= 0 && myPet.getRangedDamage() <= 0) {
            return false;
        }
        if (!currentTarget.getWorld().equals(petEntity.getWorld())) {
            return false;
        }
        if (petEntity.getLocation().distanceSquared(currentTarget.getLocation()) > 400) {
            return false;
        }
        Player owner = petEntity.getOwner().getPlayer();
        return owner != null && petEntity.getLocation().distanceSquared(owner.getLocation()) <= 600;
    }

    @Override
    public void start() {
        petEntity.setTarget(this.target, TargetPriority.Farm);
    }

    @Override
    public void stop() {
        petEntity.forgetTarget();
        target = null;
    }

    @Override
    public @NotNull GoalKey<Mob> getKey() {
        return PetGoalKey.FARM_TARGET;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.TARGET);
    }
}

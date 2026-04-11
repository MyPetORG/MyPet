package de.Keyle.MyPet.entity.ai.target;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.entity.Mob;
import de.Keyle.MyPet.api.entity.ai.target.TargetPriority;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.api.skill.skills.Behavior.BehaviorMode;
import de.Keyle.MyPet.entity.ai.PetGoalKey;
import de.Keyle.MyPet.entity.ai.movement.PetControlGoal;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * Paper {@link Goal} that auto-targets hostile entities while the owner is
 * actively driving the pet with the control item (a fishing rod by default).
 *
 * <p>Only runs while the paired {@link PetControlGoal} has an active
 * {@code moveTo} destination — i.e. the owner is still holding a
 * "go there" command. When the owner cancels control the goal releases
 * its target immediately.
 *
 * <p>Candidate filtering mirrors {@link PetAggressiveTargetGoal} but with
 * two differences:
 * <ul>
 *   <li>The search box is centred on the <em>pet</em>, not the owner,
 *       because control-mode lets the owner point at a specific area.</li>
 *   <li>{@link BehaviorMode#Raid} adds an explicit "no tamed, no pets,
 *       no players" filter so control-mode raids only attack wild mobs.</li>
 * </ul>
 *
 * <p>When a target is accepted the paired {@link PetControlGoal} is
 * notified via {@link PetControlGoal#stopControl()} so the owner's
 * move-to destination is cleared and the pet can commit to its new
 * combat target without being pulled back to the old waypoint.
 *
 * <p>Wiring: the paired {@link PetControlGoal} reference is injected
 * post-construction via {@link #setControlGoal(PetControlGoal)} because
 * both goals are constructed in the same pass and the control goal may
 * not exist yet when this target goal is built.
 *
 * <p>Declares {@link GoalType#TARGET}, making it mutually exclusive with
 * other target-acquisition goals.
 */
public class PetControlTargetGoal implements Goal<Mob> {

    private final MyPet pet;
    private final Mob mob;
    private final MyPet myPet;
    private final double range;
    private LivingEntity target;
    private PetControlGoal controlGoal;

    /**
     * @param petEntity the pet that will acquire targets while under owner control
     * @param range     horizontal radius of the "near pet" search box
     */
    public PetControlTargetGoal(MyPet pet, Mob mob, float range) {
        this.pet = pet;
        this.mob = mob;
        this.myPet = pet;
        this.range = range;
    }

    /**
     * Injects the paired {@link PetControlGoal} so this target goal can
     * query the owner's move-to destination and notify the control goal
     * when a target is accepted. Call once at wiring time.
     */
    public void setControlGoal(PetControlGoal controlGoal) {
        this.controlGoal = controlGoal;
    }

    @Override
    public boolean shouldActivate() {
        if (controlGoal == null) {
            return false;
        }
        if (myPet.getDamage() <= 0 && myPet.getRangedDamage() <= 0) {
            return false;
        }
        if (controlGoal.moveTo == null || !pet.canMove()) {
            return false;
        }
        // Cache the owner Player once. Returning null means the owner is
        // offline — every hook check below passes the owner to canHurt(),
        // which would NPE if left unguarded. With no active online owner
        // there is no meaningful "source" for the hook's PvP check anyway.
        Player owner = myPet.getOwner().getPlayer();
        if (owner == null) {
            return false;
        }
        Behavior behaviorSkill = myPet.getSkills().get(Behavior.class);
        if (behaviorSkill != null && behaviorSkill.isActive() && behaviorSkill.getBehavior() == BehaviorMode.Friendly) {
            return false;
        }

        Location petLoc = mob.getLocation();
        for (Entity entity : petLoc.getWorld().getNearbyEntities(petLoc, range, 4.0, range)) {
            if (!(entity instanceof LivingEntity living) || entity.equals(mob)) {
                continue;
            }
            if (entity instanceof ArmorStand) {
                continue;
            }
            if (entity instanceof Player targetPlayer) {
                if (myPet.getOwner().equals(targetPlayer)) {
                    continue;
                }
                if (!MyPetApi.getHookHelper().canHurt(owner, targetPlayer, true)) {
                    continue;
                }
            } else if (entity instanceof Tameable tameable && tameable.isTamed() && tameable.getOwner() != null) {
                Player tameableOwner = (Player) tameable.getOwner();
                if (myPet.getOwner().equals(tameableOwner)) {
                    continue;
                }
                if (!MyPetApi.getHookHelper().canHurt(owner, tameableOwner, true)) {
                    continue;
                }
            } else if (PetEntityMarker.isMarked(entity)) {
                MyPet targetMyPet = MyPetApi.getMyPetManager().getMyPetFromEntity(entity);
                if (targetMyPet != null && targetMyPet.getOwner() != null
                        && !MyPetApi.getHookHelper().canHurt(owner, targetMyPet.getOwner().getPlayer(), true)) {
                    continue;
                }
            }
            if (!MyPetApi.getHookHelper().canHurt(owner, living)) {
                continue;
            }
            if (behaviorSkill != null && behaviorSkill.getBehavior() == BehaviorMode.Raid) {
                if (entity instanceof Tameable || PetEntityMarker.isMarked(entity) || entity instanceof Player) {
                    continue;
                }
            }
            controlGoal.stopControl();
            this.target = living;
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldStayActive() {
        if (!pet.canMove()) {
            return false;
        }
        if (!pet.hasTarget()) {
            return false;
        }
        LivingEntity currentTarget = pet.getMyPetTarget();
        if (currentTarget == null || currentTarget.isDead()) {
            return false;
        }
        if (!currentTarget.getWorld().equals(mob.getWorld())) {
            return false;
        }
        if (mob.getLocation().distanceSquared(currentTarget.getLocation()) > 400) {
            return false;
        }
        Player owner = pet.getOwner().getPlayer();
        return owner != null && mob.getLocation().distanceSquared(owner.getLocation()) <= 600;
    }

    @Override
    public void start() {
        pet.setTarget(this.target, TargetPriority.Control);
    }

    @Override
    public void stop() {
        pet.forgetTarget();
    }

    @Override
    public @NotNull GoalKey<Mob> getKey() {
        return PetGoalKey.CONTROL_TARGET;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.TARGET);
    }
}

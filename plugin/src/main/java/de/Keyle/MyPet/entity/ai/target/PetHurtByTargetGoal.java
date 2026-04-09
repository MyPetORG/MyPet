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
import org.bukkit.entity.*;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * Paper {@link Goal} that makes a pet retaliate against whoever last
 * damaged <em>it</em>.
 *
 * <p>The source of truth for "last damager" is the event-driven
 * {@link PetDamageTracker}, which is version-independent and gives
 * consistent semantics across all supported Minecraft versions.
 *
 * <p>The activation chain is intentionally dense to mirror
 * {@link PetOwnerHurtByTargetGoal}:
 * <ul>
 *   <li>The pet must have some damage output (melee or ranged).</li>
 *   <li>The owner must be online — every subsequent
 *       {@code canHurt(owner, ...)} call would NPE with a null owner.</li>
 *   <li>The attacker must be different from the previous {@code target},
 *       so the same event is processed at most once.</li>
 *   <li>Self-attacks, armor stands, the pet's owner, pets of allies
 *       (resolved via {@code canHurt(..., true)}), and tamed allies are
 *       all rejected.</li>
 *   <li>Region-plugin protection is enforced via the final
 *       {@code canHurt(owner, target)} check.</li>
 *   <li>{@link BehaviorMode#Friendly} never retaliates;
 *       {@link BehaviorMode#Raid} retaliates only against wild mobs
 *       (not players, tamed mobs, or other pets).</li>
 * </ul>
 *
 * <p>Declares {@link GoalType#TARGET}, making it mutually exclusive with
 * other target-acquisition goals.
 */
public class PetHurtByTargetGoal implements Goal<Mob> {

    private final MyPetBukkitEntity petEntity;
    private final MyPet myPet;
    private LivingEntity target = null;

    /**
     * @param petEntity the pet that will retaliate when struck
     */
    public PetHurtByTargetGoal(MyPetBukkitEntity petEntity) {
        this.petEntity = petEntity;
        this.myPet = petEntity.getMyPet();
    }

    @Override
    public boolean shouldActivate() {
        if (myPet.getDamage() <= 0 && myPet.getRangedDamage() <= 0) {
            return false;
        }
        // The subsequent canHurt() calls pass myPet.getOwner().getPlayer() as
        // the attacker side of the hook check; that returns null when the
        // owner is offline. Bail before those derefs. Mirrors the equivalent
        // guard in PetOwnerHurtByTargetGoal.
        Player ownerPlayer = myPet.getOwner().getPlayer();
        if (ownerPlayer == null) {
            return false;
        }
        LivingEntity attacker = PetDamageTracker.getLastAttacker((LivingEntity) petEntity);
        if (attacker == null) {
            return false;
        }
        if (attacker.equals(target)) {
            // Same attacker as before, already processed
            return false;
        }
        target = attacker;
        if (target.equals(petEntity)) {
            return false;
        }
        if (target instanceof ArmorStand) {
            return false;
        }
        if (target instanceof Player targetPlayer) {
            if (targetPlayer.equals(ownerPlayer)) {
                return false;
            }
            if (!MyPetApi.getHookHelper().canHurt(ownerPlayer, targetPlayer, true)) {
                return false;
            }
        } else if (target instanceof MyPetBukkitEntity otherPet) {
            if (!MyPetApi.getHookHelper().canHurt(ownerPlayer, otherPet.getMyPet().getOwner().getPlayer(), true)) {
                return false;
            }
        } else if (target instanceof Tameable tameable) {
            if (tameable.isTamed() && tameable.getOwner() != null) {
                Player tameableOwner = (Player) tameable.getOwner();
                if (myPet.getOwner().equals(tameableOwner)) {
                    return false;
                }
            }
        }
        if (!MyPetApi.getHookHelper().canHurt(ownerPlayer, target)) {
            return false;
        }
        // Behavior mode enforcement — must mirror PetOwnerHurtByTargetGoal so
        // Friendly pets never retaliate and Raid pets only retaliate against
        // wild mobs (not players, tamed animals, or other MyPet pets).
        Behavior behaviorSkill = myPet.getSkills().get(Behavior.class);
        if (behaviorSkill != null && behaviorSkill.isActive()) {
            if (behaviorSkill.getBehavior() == BehaviorMode.Friendly) {
                return false;
            }
            if (behaviorSkill.getBehavior() == BehaviorMode.Raid) {
                if (target instanceof Tameable tameable && tameable.isTamed()) {
                    return false;
                }
                if (target instanceof MyPetBukkitEntity) {
                    return false;
                }
                return !(target instanceof Player);
            }
        }
        return true;
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
        petEntity.setTarget(this.target, TargetPriority.GetHurt);
    }

    @Override
    public void stop() {
        petEntity.forgetTarget();
    }

    @Override
    public @NotNull GoalKey<Mob> getKey() {
        return PetGoalKey.HURT_BY_TARGET;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.TARGET);
    }
}

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
 * Paper {@link Goal} that acquires a hostile target for a pet whose
 * {@link Behavior} skill is set to {@link BehaviorMode#Aggressive}.
 *
 * <p>Scans entities near the owner (not near the pet), which produces the
 * intuitive "guard my owner" behaviour that players expect from aggressive
 * pets. Each candidate must pass every one of the following filters:
 * <ul>
 *   <li>It is a {@link LivingEntity} other than the pet itself.</li>
 *   <li>It is not an {@link ArmorStand} and not dead.</li>
 *   <li>The pet-to-candidate distance is at most ~9.5 blocks (91 m²).</li>
 *   <li>Ownership filtering rejects the owner's own player, other pets
 *       owned by allies (via {@code canHurt(..., true)}), and tamed
 *       mobs belonging to the owner or allies.</li>
 *   <li>A final unconditional
 *       {@code canHurt(owner, living)} check enforces region-plugin
 *       protection (WorldGuard, GriefPrevention, …) and catches untamed
 *       {@link Tameable} entities that slipped past the ownership chain.</li>
 * </ul>
 *
 * <p>Once selected, the target is installed via
 * {@link MyPetBukkitEntity#setTarget(LivingEntity, TargetPriority)} at
 * {@link TargetPriority#Aggressive}; the attack goals then pick the target
 * up on their own. The goal stops when the target dies, teleports worlds,
 * or the pet drifts more than ~20 blocks from it / more than ~24.5 blocks
 * from the owner.
 *
 * <p>Declares {@link GoalType#TARGET}, making it mutually exclusive with
 * the other target-acquisition goals in this package.
 */
public class PetAggressiveTargetGoal implements Goal<Mob> {

    private final MyPetBukkitEntity petEntity;
    private final MyPet myPet;
    private final double range;
    private LivingEntity target;

    /**
     * @param petEntity the pet that will acquire targets
     * @param range     radius (in blocks) of the "near owner" search box
     */
    public PetAggressiveTargetGoal(MyPetBukkitEntity petEntity, float range) {
        this.petEntity = petEntity;
        this.myPet = petEntity.getMyPet();
        this.range = range;
    }

    @Override
    public boolean shouldActivate() {
        Behavior behaviorSkill = myPet.getSkills().get(Behavior.class);
        if (!behaviorSkill.isActive() || behaviorSkill.getBehavior() != BehaviorMode.Aggressive) {
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
            if (!(entity instanceof LivingEntity living) || entity.equals(petEntity)) {
                continue;
            }
            if (entity instanceof ArmorStand || living.isDead()) {
                continue;
            }
            if (petLoc.distanceSquared(living.getLocation()) > 91) {
                continue;
            }
            if (entity instanceof Player targetPlayer) {
                if (myPet.getOwner().equals(targetPlayer)) {
                    continue;
                }
                if (!MyPetApi.getHookHelper().canHurt(owner, targetPlayer, true)) {
                    continue;
                }
            } else if (entity instanceof MyPetBukkitEntity otherPet) {
                if (!MyPetApi.getHookHelper().canHurt(owner, otherPet.getMyPet().getOwner().getPlayer(), true)) {
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
            }
            // Final unconditional canHurt check — matches PetControlTargetGoal.
            // The if/else chain above handled *ownership* filtering (player vs
            // pet vs tamed); this check enforces region-plugin protection
            // (WorldGuard, GriefPrevention, etc.) on the entity itself,
            // including untamed Tameables (wild wolves, wild cats) that would
            // otherwise slip past the chain.
            if (!MyPetApi.getHookHelper().canHurt(owner, living)) {
                continue;
            }
            this.target = living;
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
        if (behaviorSkill.getBehavior() != BehaviorMode.Aggressive) {
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
        petEntity.setTarget(this.target, TargetPriority.Aggressive);
    }

    @Override
    public void stop() {
        petEntity.forgetTarget();
        target = null;
    }

    @Override
    public @NotNull GoalKey<Mob> getKey() {
        return PetGoalKey.AGGRESSIVE_TARGET;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.TARGET);
    }
}

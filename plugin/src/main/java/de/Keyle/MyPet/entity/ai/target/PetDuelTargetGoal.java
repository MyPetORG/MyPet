package de.Keyle.MyPet.entity.ai.target;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.entity.ai.target.TargetPriority;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.api.skill.skills.Behavior.BehaviorMode;
import de.Keyle.MyPet.entity.ai.PetGoalKey;
import de.Keyle.MyPet.skill.skills.BehaviorImpl;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.EnumSet;

/**
 * Paper {@link Goal} that pairs up two MyPets whose owners have both set
 * {@link Behavior} to {@link BehaviorMode#Duel}, locking each pet onto
 * the other as a one-on-one target.
 *
 * <p>On first activation the goal scans entities near the owner for
 * another {@link MyPetBukkitEntity} that:
 * <ul>
 *   <li>is alive, movable, and has its Behavior skill active in
 *       {@code Duel} mode,</li>
 *   <li>can deal either melee or ranged damage (ranged-only builds are
 *       explicitly eligible — a pet built around a bow/crossbow
 *       skilltree can still duel).</li>
 * </ul>
 *
 * <p>Once a duel pair is chosen, {@link #start()} installs the target on
 * both sides of the duel via the Paper {@code MobGoals} lookup:
 * {@code this.duelOpponent} and the opposite pet's {@code duelOpponent}
 * are wired together so neither side needs to scan again on subsequent
 * ticks — they just read the cached opponent.
 *
 * <p>{@link #stop()} is symmetric: it clears both pets'
 * {@code duelOpponent} fields. Without the symmetric de-wire the other
 * pet's {@link #shouldActivate()} would see its {@code duelOpponent}
 * still non-null on the next tick and instantly re-engage — producing an
 * infinite duel loop even after the first pet despawned or changed
 * behavior mode.
 *
 * <p>Declares {@link GoalType#TARGET}, making it mutually exclusive with
 * other target-acquisition goals.
 */
public class PetDuelTargetGoal implements Goal<Mob> {

    private final MyPetBukkitEntity petEntity;
    private final MyPet myPet;
    private final double range;
    private MyPetBukkitEntity target;
    private MyPetBukkitEntity duelOpponent = null;

    /**
     * @param petEntity the pet that will look for a duel partner
     * @param range     radius (in blocks) of the "near owner" search box
     */
    public PetDuelTargetGoal(MyPetBukkitEntity petEntity, float range) {
        this.petEntity = petEntity;
        this.myPet = petEntity.getMyPet();
        this.range = range;
    }

    @Override
    public boolean shouldActivate() {
        Behavior behaviorSkill = myPet.getSkills().get(Behavior.class);
        if (!behaviorSkill.isActive() || behaviorSkill.getBehavior() != BehaviorMode.Duel) {
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
        if (duelOpponent != null) {
            this.target = duelOpponent;
            return true;
        }

        Player owner = petEntity.getOwner().getPlayer();
        if (owner == null) {
            return false;
        }
        Location ownerLoc = owner.getLocation();

        Collection<Entity> nearby = ownerLoc.getWorld().getNearbyEntities(ownerLoc, range, range, range);
        for (Entity entity : nearby) {
            if (!(entity instanceof MyPetBukkitEntity otherPet) || entity.equals(petEntity)) {
                continue;
            }
            if (otherPet.isDead()) {
                continue;
            }
            MyPet targetMyPet = otherPet.getMyPet();
            if (!targetMyPet.getSkills().isActive(BehaviorImpl.class)) {
                continue;
            }
            if (!otherPet.canMove()) {
                continue;
            }
            BehaviorImpl targetBehavior = targetMyPet.getSkills().get(BehaviorImpl.class);
            if (targetBehavior.getBehavior() != BehaviorMode.Duel) {
                continue;
            }
            // Accept opponents that can deal damage via either melee or ranged,
            // matching the self-eligibility check at the top of this method.
            // A pet built purely around a bow/crossbow skilltree is a valid
            // duel partner.
            if (targetMyPet.getDamage() == 0 && targetMyPet.getRangedDamage() == 0) {
                continue;
            }
            this.target = otherPet;
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
        if (behaviorSkill.getBehavior() != BehaviorMode.Duel) {
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
        petEntity.setTarget(this.target, TargetPriority.Duel);
        setDuelOpponent(this.target);
        Goal<Mob> opponentGoal = Bukkit.getMobGoals().getGoal((Mob) target, PetGoalKey.DUEL_TARGET);
        if (opponentGoal instanceof PetDuelTargetGoal opponentDuelGoal) {
            opponentDuelGoal.setDuelOpponent(this.petEntity);
        }
    }

    @Override
    public void stop() {
        // Symmetric de-wire: start() wired both pets' duelOpponent fields, so
        // stop() must clear BOTH. Without this, the other pet's shouldActivate()
        // sees its own duelOpponent != null on the next tick and instantly
        // re-engages, causing the duel to loop forever even after the first
        // pet has been despawned or switched behavior mode.
        MyPetBukkitEntity opponent = duelOpponent != null ? duelOpponent : target;
        if (opponent != null) {
            Goal<Mob> opponentGoal = Bukkit.getMobGoals().getGoal((Mob) opponent, PetGoalKey.DUEL_TARGET);
            if (opponentGoal instanceof PetDuelTargetGoal opponentDuelGoal && opponentDuelGoal != this) {
                opponentDuelGoal.duelOpponent = null;
            }
        }
        petEntity.forgetTarget();
        duelOpponent = null;
        target = null;
    }

    /** @return the pet currently being dueled, or {@code null} if none */
    public MyPetBukkitEntity getDuelOpponent() {
        return duelOpponent;
    }

    /**
     * Sets the cached duel opponent. Called by {@link #start()} on both
     * sides of a duel so each pet can resolve its partner without
     * rescanning the entity list on subsequent ticks.
     */
    public void setDuelOpponent(MyPetBukkitEntity opponent) {
        this.duelOpponent = opponent;
    }

    @Override
    public @NotNull GoalKey<Mob> getKey() {
        return PetGoalKey.DUEL_TARGET;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.TARGET);
    }
}

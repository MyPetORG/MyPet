/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
 *
 * MyPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.entity.ai.target;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.entity.Mob;
import de.Keyle.MyPet.api.entity.ai.target.TargetPriority;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.api.skill.skills.Behavior.BehaviorMode;
import de.Keyle.MyPet.entity.ai.PetGoalKey;
import de.Keyle.MyPet.entity.ai.PetGoalWorlds;
import de.Keyle.MyPet.skill.skills.BehaviorImpl;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;

import static de.Keyle.MyPet.MyPetApi.getPetManager;

/**
 * Paper {@link Goal} that pairs up two Pets whose owners have both set
 * {@link Behavior} to {@link BehaviorMode#Duel}, locking each pet onto
 * the other as a one-on-one target.
 *
 * <p>On first activation the goal scans entities near the owner for
 * another {@link Pet} that:
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

    private final Pet pet;
    private final Mob mob;
    private final double range;
    private Mob target;
    private Mob duelOpponent = null;
    private int rescanCooldown = 0;

    /**
     * @param petEntity the pet that will look for a duel partner
     * @param range     radius (in blocks) of the "near owner" search box
     */
    public PetDuelTargetGoal(Pet pet, Mob mob, float range) {
        this.pet = pet;
        this.mob = mob;
        this.range = range;
    }

    @Override
    public boolean shouldActivate() {
        if (!Bukkit.isOwnedByCurrentRegion(mob)) {
            return false;
        }
        Behavior behaviorSkill = pet.getSkills().get(Behavior.class);
        if (!behaviorSkill.isActive() || behaviorSkill.getBehavior() != BehaviorMode.Duel) {
            return false;
        }
        if (pet.getDamage() <= 0 && pet.getRangedDamage() <= 0) {
            return false;
        }
        if (!pet.canMove()) {
            return false;
        }
        if (pet.hasTarget()) {
            return false;
        }
        if (duelOpponent != null) {
            this.target = duelOpponent;
            return true;
        }

        Player owner = pet.getOwner().getPlayer();
        if (owner == null) {
            return false;
        }
        // Rescan every ~10 ticks — the AABB scan is too expensive for every tick.
        if (rescanCooldown > 0) {
            rescanCooldown--;
            return false;
        }
        rescanCooldown = 10 + ThreadLocalRandom.current().nextInt(5);
        // Scan around the pet (owning region thread). Scanning around the owner would touch
        // the owner's region from the pet's thread on Folia.
        Location petLoc = mob.getLocation();

        Collection<Entity> nearby = mob.getWorld().getNearbyEntities(petLoc, range, range, range);
        for (Entity entity : nearby) {
            if (!PetEntityMarker.isMarked(entity) || entity.equals(mob)) {
                continue;
            }
            if (!(entity instanceof Mob otherMob) || otherMob.isDead()) {
                continue;
            }
            Pet targetPet = getPetManager().getPetFromEntity(otherMob);
            if (targetPet == null) {
                continue;
            }
            if (!targetPet.getSkills().isActive(BehaviorImpl.class)) {
                continue;
            }
            if (!targetPet.canMove()) {
                continue;
            }
            BehaviorImpl targetBehavior = targetPet.getSkills().get(BehaviorImpl.class);
            if (targetBehavior.getBehavior() != BehaviorMode.Duel) {
                continue;
            }
            if (targetPet.getDamage() == 0 && targetPet.getRangedDamage() == 0) {
                continue;
            }
            this.target = otherMob;
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldStayActive() {
        if (!Bukkit.isOwnedByCurrentRegion(mob)) {
            return false;
        }
        if (!pet.canMove()) {
            return false;
        }
        if (!pet.hasTarget()) {
            return false;
        }
        LivingEntity currentTarget = pet.getPetTarget();
        if (currentTarget == null || currentTarget.isDead()) {
            return false;
        }
        Behavior behaviorSkill = pet.getSkills().get(Behavior.class);
        if (behaviorSkill.getBehavior() != BehaviorMode.Duel) {
            return false;
        }
        if (pet.getDamage() <= 0 && pet.getRangedDamage() <= 0) {
            return false;
        }
        if (!currentTarget.getWorld().equals(mob.getWorld())) {
            return false;
        }
        if (mob.getLocation().distanceSquared(currentTarget.getLocation()) > 400) {
            return false;
        }
        Player owner = pet.getOwner().getPlayer();
        // The target world check above says nothing about the owner, who can have
        // changed world independently; measuring distance to them would then throw.
        return owner != null && !PetGoalWorlds.isCrossWorld(mob, owner)
                && mob.getLocation().distanceSquared(owner.getLocation()) <= 600;
    }

    @Override
    public void start() {
        pet.setTarget(this.target, TargetPriority.Duel);
        setDuelOpponent(this.target);
        Goal<Mob> opponentGoal = Bukkit.getMobGoals().getGoal(target, PetGoalKey.DUEL_TARGET);
        if (opponentGoal instanceof PetDuelTargetGoal opponentDuelGoal) {
            opponentDuelGoal.setDuelOpponent(mob);
        }
    }

    @Override
    public void stop() {
        Mob opponent = duelOpponent != null ? duelOpponent : target;
        if (opponent != null) {
            Goal<Mob> opponentGoal = Bukkit.getMobGoals().getGoal(opponent, PetGoalKey.DUEL_TARGET);
            if (opponentGoal instanceof PetDuelTargetGoal opponentDuelGoal && opponentDuelGoal != this) {
                opponentDuelGoal.duelOpponent = null;
            }
        }
        pet.forgetTarget();
        duelOpponent = null;
        target = null;
        rescanCooldown = 0;
    }

    /** @return the pet currently being dueled, or {@code null} if none */
    public Mob getDuelOpponent() {
        return duelOpponent;
    }

    /**
     * Sets the cached duel opponent. Called by {@link #start()} on both
     * sides of a duel so each pet can resolve its partner without
     * rescanning the entity list on subsequent ticks.
     */
    public void setDuelOpponent(Mob opponent) {
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

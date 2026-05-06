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
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.entity.spawn.PetEntityMarker;
import org.bukkit.entity.Mob;
import de.Keyle.MyPet.api.entity.ai.target.TargetPriority;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.api.skill.skills.Behavior.BehaviorMode;
import de.Keyle.MyPet.entity.ai.PetGoalKey;
import org.bukkit.Bukkit;
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

    private final Pet pet;
    private final Mob mob;
    private LivingEntity target = null;

    /**
     * @param petEntity the pet that will retaliate when struck
     */
    public PetHurtByTargetGoal(Pet pet, Mob mob) {
        this.pet = pet;
        this.mob = mob;
    }

    @Override
    public boolean shouldActivate() {
        if (!Bukkit.isOwnedByCurrentRegion(mob)) {
            return false;
        }
        if (pet.getDamage() <= 0 && pet.getRangedDamage() <= 0) {
            return false;
        }
        // The subsequent canHurt() calls pass pet.getOwner().getPlayer() as
        // the attacker side of the hook check; that returns null when the
        // owner is offline. Bail before those derefs. Mirrors the equivalent
        // guard in PetOwnerHurtByTargetGoal.
        Player ownerPlayer = pet.getOwner().getPlayer();
        if (ownerPlayer == null) {
            return false;
        }
        LivingEntity attacker = PetDamageTracker.getLastAttacker(mob);
        if (attacker == null) {
            return false;
        }
        if (attacker.equals(target)) {
            // Same attacker as before, already processed
            return false;
        }
        target = attacker;
        if (target.equals(mob)) {
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
        } else if (PetEntityMarker.isMarked(target)) {
            Pet otherPet = MyPetApi.getPetManager().getPetFromEntity(target);
            if (otherPet != null && otherPet.getOwner() != null
                    && !MyPetApi.getHookHelper().canHurt(ownerPlayer, otherPet.getOwner().getPlayer(), true)) {
                return false;
            }
        } else if (target instanceof Tameable tameable) {
            if (tameable.isTamed() && tameable.getOwner() != null) {
                Player tameableOwner = (Player) tameable.getOwner();
                if (pet.getOwner().equals(tameableOwner)) {
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
        Behavior behaviorSkill = pet.getSkills().get(Behavior.class);
        if (behaviorSkill != null && behaviorSkill.isActive()) {
            if (behaviorSkill.getBehavior() == BehaviorMode.Friendly) {
                return false;
            }
            if (behaviorSkill.getBehavior() == BehaviorMode.Raid) {
                if (target instanceof Tameable tameable && tameable.isTamed()) {
                    return false;
                }
                if (PetEntityMarker.isMarked(target)) {
                    return false;
                }
                return !(target instanceof Player);
            }
        }
        return true;
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
        pet.setTarget(this.target, TargetPriority.GetHurt);
    }

    @Override
    public void stop() {
        pet.forgetTarget();
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

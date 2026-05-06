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
import de.Keyle.MyPet.api.entity.MyPet;
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
 * damaged its <em>owner</em> — the "protect-my-human" instinct. Mirrors
 * {@link PetHurtByTargetGoal} but keys on the owner's attacker instead
 * of the pet's.
 *
 * <p>Source of truth is {@link PetDamageTracker#getLastAttacker}, which
 * is populated by the event listener on {@code EntityDamageByEntityEvent}
 * — the tracker entry for the owner is set the instant a mob/player hits
 * them, so this goal can pick up the retaliation on the very next tick.
 *
 * <p>Filtering mirrors {@link PetHurtByTargetGoal}:
 * <ul>
 *   <li>Requires non-zero damage output (melee or ranged).</li>
 *   <li>Rejects self-attacks, armor stands, the owner themselves, and
 *       tamed mobs belonging to the owner.</li>
 *   <li>Rejects pets of allies via {@code canHurt(..., true)}.</li>
 *   <li>Enforces region-plugin protection via a final unconditional
 *       {@code canHurt(owner, lastDamager)} check.</li>
 *   <li>{@link BehaviorMode#Friendly} never retaliates;
 *       {@link BehaviorMode#Raid} only retaliates against wild mobs.</li>
 * </ul>
 *
 * <p>Declares {@link GoalType#TARGET}, making it mutually exclusive with
 * other target-acquisition goals.
 */
public class PetOwnerHurtByTargetGoal implements Goal<Mob> {

    private final MyPet pet;
    private final Mob mob;
    private final MyPet myPet;
    private LivingEntity lastDamager;

    /**
     * @param petEntity the pet that will retaliate when its owner is struck
     */
    public PetOwnerHurtByTargetGoal(MyPet pet, Mob mob) {
        this.pet = pet;
        this.mob = mob;
        this.myPet = pet;
    }

    @Override
    public boolean shouldActivate() {
        if (!Bukkit.isOwnedByCurrentRegion(mob)) {
            return false;
        }
        if (!pet.canMove()) {
            return false;
        }
        if (myPet.getDamage() <= 0 && myPet.getRangedDamage() <= 0) {
            return false;
        }
        Player owner = pet.getOwner().getPlayer();
        if (owner == null) {
            return false;
        }
        this.lastDamager = PetDamageTracker.getLastAttacker(owner);
        if (this.lastDamager == null || this.lastDamager.isDead()) {
            return false;
        }
        if (this.lastDamager instanceof ArmorStand) {
            return false;
        }
        if (this.lastDamager.equals(mob)) {
            return false;
        }
        if (this.lastDamager instanceof Player targetPlayer) {
            if (owner.equals(targetPlayer)) {
                return false;
            }
            if (!MyPetApi.getHookHelper().canHurt(owner, targetPlayer, true)) {
                return false;
            }
        } else if (PetEntityMarker.isMarked(lastDamager)) {
            MyPet otherPet = MyPetApi.getPetManager().getMyPetFromEntity(lastDamager);
            if (otherPet != null && otherPet.getOwner() != null
                    && !MyPetApi.getHookHelper().canHurt(owner, otherPet.getOwner().getPlayer(), true)) {
                return false;
            }
        } else if (this.lastDamager instanceof Tameable tameable) {
            if (tameable.isTamed() && tameable.getOwner() != null) {
                Player tameableOwner = (Player) tameable.getOwner();
                if (myPet.getOwner().equals(tameableOwner)) {
                    return false;
                }
            }
        }
        if (!MyPetApi.getHookHelper().canHurt(owner, lastDamager)) {
            return false;
        }
        Behavior behaviorSkill = myPet.getSkills().get(Behavior.class);
        if (behaviorSkill != null && behaviorSkill.isActive()) {
            if (behaviorSkill.getBehavior() == BehaviorMode.Friendly) {
                return false;
            }
            if (behaviorSkill.getBehavior() == BehaviorMode.Raid) {
                if (lastDamager instanceof Tameable tameable && tameable.isTamed()) {
                    return false;
                }
                if (PetEntityMarker.isMarked(lastDamager)) {
                    return false;
                }
                return !(lastDamager instanceof Player);
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
        LivingEntity target = pet.getMyPetTarget();
        if (target == null || target.isDead()) {
            return false;
        }
        if (!target.getWorld().equals(mob.getWorld())) {
            return false;
        }
        if (mob.getLocation().distanceSquared(target.getLocation()) > 400) {
            return false;
        }
        Player owner = pet.getOwner().getPlayer();
        return owner != null && mob.getLocation().distanceSquared(owner.getLocation()) <= 600;
    }

    @Override
    public void start() {
        pet.setTarget(this.lastDamager, TargetPriority.OwnerGetsHurt);
    }

    @Override
    public void stop() {
        pet.forgetTarget();
    }

    @Override
    public @NotNull GoalKey<Mob> getKey() {
        return PetGoalKey.OWNER_HURT_BY_TARGET;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.TARGET);
    }
}

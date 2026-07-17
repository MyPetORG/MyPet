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
import org.bukkit.entity.Mob;
import de.Keyle.MyPet.api.entity.ai.target.TargetPriority;
import de.Keyle.MyPet.api.skill.skills.Behavior;
import de.Keyle.MyPet.api.skill.skills.Behavior.BehaviorMode;
import de.Keyle.MyPet.entity.ai.PetGoalKey;
import de.Keyle.MyPet.entity.ai.PetGoalWorlds;
import org.bukkit.Bukkit;
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

    private final Pet pet;
    private final Mob mob;
    private final double range;
    private LivingEntity target;

    /**
     * @param petEntity the pet that will acquire monster targets while in Farm mode
     * @param range     radius (in blocks) of the "near owner" search box
     */
    public PetFarmTargetGoal(Pet pet, Mob mob, float range) {
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
        if (!behaviorSkill.isActive() || behaviorSkill.getBehavior() != BehaviorMode.Farm) {
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

        Player owner = pet.getOwner().getPlayer();
        if (owner == null) {
            return false;
        }
        Location petLoc = mob.getLocation();
        // Scan around the pet (owning region thread). Scanning around the owner would touch
        // the owner's region from the pet's thread on Folia. The distance-to-pet filter
        // below already restricts results to a small radius of the pet.

        for (Entity entity : mob.getWorld().getNearbyEntities(petLoc, range, range, range)) {
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
        if (behaviorSkill.getBehavior() != BehaviorMode.Farm) {
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
        pet.setTarget(this.target, TargetPriority.Farm);
    }

    @Override
    public void stop() {
        pet.forgetTarget();
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

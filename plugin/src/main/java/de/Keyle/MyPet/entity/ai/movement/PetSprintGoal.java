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

package de.Keyle.MyPet.entity.ai.movement;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import de.Keyle.MyPet.api.entity.MyPet;
import org.bukkit.entity.Mob;
import de.Keyle.MyPet.entity.ai.PetGoalKey;
import de.Keyle.MyPet.skill.skills.SprintImpl;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
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

    private final MyPet pet;
    private final Mob mob;
    private final MyPet myPet;
    private final float walkSpeedModifier;
    private LivingEntity lastTarget = null;

    /**
     * @param petEntity         the pet whose chase should get the sprint boost
     * @param walkSpeedModifier multiplicative navigation speed modifier applied while sprinting
     */
    public PetSprintGoal(MyPet pet, Mob mob, float walkSpeedModifier) {
        this.pet = pet;
        this.mob = mob;
        this.myPet = pet;
        this.walkSpeedModifier = walkSpeedModifier;
    }

    @Override
    public boolean shouldActivate() {
        if (!Bukkit.isOwnedByCurrentRegion(mob)) {
            return false;
        }
        if (!myPet.getSkills().isActive(SprintImpl.class)) {
            return false;
        }
        if (myPet.getDamage() <= 0) {
            return false;
        }
        if (!pet.hasTarget()) {
            return false;
        }
        LivingEntity target = pet.getMyPetTarget();
        if (target == null || target.isDead()) {
            return false;
        }
        if (target.equals(lastTarget)) {
            return false;
        }
        if (myPet.getRangedDamage() > 0 && mob.getLocation().distanceSquared(target.getLocation()) >= 16) {
            return false;
        }
        this.lastTarget = target;
        return true;
    }

    @Override
    public boolean shouldStayActive() {
        if (!Bukkit.isOwnedByCurrentRegion(mob)) {
            return false;
        }
        if (pet.getOwner() == null) {
            return false;
        }
        if (lastTarget == null || lastTarget.isDead()) {
            return false;
        }
        if (mob.getLocation().distanceSquared(lastTarget.getLocation()) < 16) {
            return false;
        }
        return pet.canMove();
    }

    @Override
    public void start() {
        pet.getPetNavigation().getParameters().addSpeedModifier("Sprint", walkSpeedModifier);
    }

    @Override
    public void stop() {
        pet.getPetNavigation().getParameters().removeSpeedModifier("Sprint");
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

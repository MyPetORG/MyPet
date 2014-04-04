/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

import de.Keyle.MyPet.entity.ai.AIGoal;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.skill.skills.implementation.Behavior;
import de.Keyle.MyPet.skill.skills.info.BehaviorInfo.BehaviorState;
import net.minecraft.server.v1_7_R2.EntityPlayer;
import org.bukkit.craftbukkit.v1_7_R2.entity.CraftPlayer;

public class BehaviorDuelTarget extends AIGoal {
    private MyPet myPet;
    private EntityMyPet petEntity;
    private EntityPlayer petOwnerEntity;
    private EntityMyPet target;
    private EntityMyPet duelOpponent = null;
    private float range;
    private Behavior behaviorSkill = null;

    public BehaviorDuelTarget(EntityMyPet petEntity, float range) {
        this.petEntity = petEntity;
        this.petOwnerEntity = ((CraftPlayer) petEntity.getOwner().getPlayer()).getHandle();
        this.myPet = petEntity.getMyPet();
        this.range = range;
        if (myPet.getSkills().hasSkill(Behavior.class)) {
            behaviorSkill = myPet.getSkills().getSkill(Behavior.class);
        }
    }

    @Override
    public boolean shouldStart() {
        if (behaviorSkill == null || !behaviorSkill.isActive() || behaviorSkill.getBehavior() != BehaviorState.Duel) {
            return false;
        }
        if (myPet.getDamage() <= 0 && myPet.getRangedDamage() <= 0) {
            return false;
        }
        if (!myPet.getCraftPet().canMove()) {
            return false;
        }
        if (petEntity.getGoalTarget() != null && petEntity.getGoalTarget().isAlive()) {
            return false;
        }
        if (duelOpponent != null) {
            this.target = duelOpponent;
            return true;
        }

        for (Object entityObj : this.petEntity.world.a(EntityMyPet.class, this.petOwnerEntity.boundingBox.grow((double) range, (double) range, (double) range))) {
            EntityMyPet entityMyPet = (EntityMyPet) entityObj;
            MyPet targetMyPet = entityMyPet.getMyPet();

            if (entityMyPet != petEntity && entityMyPet.isAlive()) {
                if (!targetMyPet.getSkills().isSkillActive(Behavior.class) || !targetMyPet.getCraftPet().canMove()) {
                    continue;
                }
                Behavior targetbehavior = targetMyPet.getSkills().getSkill(Behavior.class);
                if (targetbehavior.getBehavior() != BehaviorState.Duel) {
                    continue;
                }
                if (targetMyPet.getDamage() == 0) {
                    continue;
                }
                this.target = entityMyPet;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldFinish() {
        if (!petEntity.canMove()) {
            return true;
        } else if (petEntity.getGoalTarget() == null) {
            return true;
        } else if (!petEntity.getGoalTarget().isAlive()) {
            return true;
        } else if (behaviorSkill.getBehavior() != BehaviorState.Duel) {
            return true;
        } else if (myPet.getDamage() <= 0 && myPet.getRangedDamage() <= 0) {
            return true;
        } else if (petEntity.getGoalTarget().world != petEntity.world) {
            return true;
        } else if (petEntity.f(petEntity.getGoalTarget()) > 400) {
            return true;
        } else if (petEntity.f(petEntity.getOwner().getEntityPlayer()) > 600) {
            return true;
        }
        return false;
    }

    @Override
    public void start() {
        petEntity.setGoalTarget(this.target);
        setDuelOpponent(this.target);
        if (target.petTargetSelector.hasGoal("DuelTarget")) {
            BehaviorDuelTarget duelGoal = (BehaviorDuelTarget) target.petTargetSelector.getGoal("DuelTarget");
            duelGoal.setDuelOpponent(this.petEntity);
        }
    }

    @Override
    public void finish() {
        petEntity.setGoalTarget(null);
        duelOpponent = null;
        target = null;
    }

    public EntityMyPet getDuelOpponent() {
        return duelOpponent;
    }

    public void setDuelOpponent(EntityMyPet opponent) {
        this.duelOpponent = opponent;
    }
}
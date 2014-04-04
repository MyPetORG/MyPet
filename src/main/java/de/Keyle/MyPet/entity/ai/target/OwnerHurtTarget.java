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
import de.Keyle.MyPet.util.support.PvPChecker;
import net.minecraft.server.v1_7_R2.EntityLiving;
import net.minecraft.server.v1_7_R2.EntityPlayer;
import net.minecraft.server.v1_7_R2.EntityTameableAnimal;
import org.bukkit.entity.Player;

public class OwnerHurtTarget extends AIGoal {

    EntityMyPet petEntity;
    EntityLiving target;
    MyPet myPet;
    private Behavior behaviorSkill = null;

    public OwnerHurtTarget(EntityMyPet petEntity) {
        this.petEntity = petEntity;
        this.myPet = petEntity.getMyPet();
        if (myPet.getSkills().hasSkill(Behavior.class)) {
            behaviorSkill = myPet.getSkills().getSkill(Behavior.class);
        }
    }

    @Override
    public boolean shouldStart() {
        if (!petEntity.canMove()) {
            return false;
        }
        if (myPet.getDamage() <= 0 && myPet.getRangedDamage() <= 0) {
            return false;
        }
        if (this.petEntity.goalTarget == null) {
            return false;
        }
        if (behaviorSkill != null && behaviorSkill.isActive()) {
            if (behaviorSkill.getBehavior() == Behavior.BehaviorState.Friendly) {
                this.petEntity.goalTarget = null;
                return false;
            }
            if (behaviorSkill.getBehavior() == BehaviorState.Raid) {
                if (this.petEntity.goalTarget instanceof EntityTameableAnimal && ((EntityTameableAnimal) this.petEntity.goalTarget).isTamed()) {
                    this.petEntity.goalTarget = null;
                    return false;
                }
                if (this.petEntity.goalTarget instanceof EntityMyPet) {
                    this.petEntity.goalTarget = null;
                    return false;
                }
                if (this.petEntity.goalTarget instanceof EntityPlayer) {
                    this.petEntity.goalTarget = null;
                    return false;
                }
            }
        }
        if (this.petEntity.goalTarget instanceof EntityPlayer) {
            Player targetPlayer = (Player) this.petEntity.goalTarget.getBukkitEntity();

            if (myPet.getOwner().equals(targetPlayer)) {
                this.petEntity.goalTarget = null;
                return false;
            } else if (!PvPChecker.canHurt(myPet.getOwner().getPlayer(), targetPlayer)) {
                this.petEntity.goalTarget = null;
                return false;
            }
        } else if (this.petEntity.goalTarget instanceof EntityTameableAnimal) {
            EntityTameableAnimal tameable = (EntityTameableAnimal) this.petEntity.goalTarget;
            if (tameable.isTamed() && tameable.getOwner() != null) {
                Player tameableOwner = (Player) tameable.getOwner().getBukkitEntity();
                if (myPet.getOwner().equals(tameableOwner)) {
                    this.petEntity.goalTarget = null;
                    return false;
                } else if (!PvPChecker.canHurt(myPet.getOwner().getPlayer(), tameableOwner)) {
                    this.petEntity.goalTarget = null;
                    return false;
                }
            }
        } else if (this.petEntity.goalTarget instanceof EntityMyPet) {
            MyPet targetMyPet = ((EntityMyPet) this.petEntity.goalTarget).getMyPet();
            if (targetMyPet == null) {
                this.petEntity.goalTarget = null;
                return false;
            }
            if (!PvPChecker.canHurt(myPet.getOwner().getPlayer(), targetMyPet.getOwner().getPlayer())) {
                this.petEntity.goalTarget = null;
                return false;
            }
        }
        if (!PvPChecker.canHurtCitizens(this.petEntity.goalTarget.getBukkitEntity())) {
            return false;
        }
        this.target = this.petEntity.goalTarget;
        this.petEntity.goalTarget = null;
        return true;
    }

    @Override
    public boolean shouldFinish() {
        EntityLiving entityliving = petEntity.getGoalTarget();

        if (!petEntity.canMove()) {
            return true;
        } else if (entityliving == null) {
            return true;
        } else if (!entityliving.isAlive()) {
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
    }

    @Override
    public void finish() {
        petEntity.setGoalTarget(null);
    }
}
/*
 * This file is part of mypet-v1_8_R3
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet-v1_8_R3 is licensed under the GNU Lesser General Public License.
 *
 * mypet-v1_8_R3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet-v1_8_R3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.compat.v1_8_R3.entity.ai.target;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.ActiveMyPet;
import de.Keyle.MyPet.api.entity.ai.AIGoal;
import de.Keyle.MyPet.api.skill.skills.BehaviorInfo.BehaviorState;
import de.Keyle.MyPet.compat.v1_8_R3.entity.EntityMyPet;
import de.Keyle.MyPet.skill.skills.Behavior;
import net.minecraft.server.v1_8_R3.EntityArmorStand;
import net.minecraft.server.v1_8_R3.EntityLiving;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.EntityTameableAnimal;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTargetEvent;

public class OwnerHurtTarget extends AIGoal {

    EntityMyPet petEntity;
    EntityLiving target;
    ActiveMyPet myPet;
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
        if (this.petEntity.goalTarget instanceof EntityArmorStand) {
            this.petEntity.goalTarget = null;
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
            } else if (!MyPetApi.getHookManager().canHurt(myPet.getOwner().getPlayer(), targetPlayer, true)) {
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
                } else if (!MyPetApi.getHookManager().canHurt(myPet.getOwner().getPlayer(), tameableOwner, true)) {
                    this.petEntity.goalTarget = null;
                    return false;
                }
            }
        } else if (this.petEntity.goalTarget instanceof EntityMyPet) {
            ActiveMyPet targetMyPet = ((EntityMyPet) this.petEntity.goalTarget).getMyPet();
            if (targetMyPet == null) {
                this.petEntity.goalTarget = null;
                return false;
            }
            if (!MyPetApi.getHookManager().canHurt(myPet.getOwner().getPlayer(), targetMyPet.getOwner().getPlayer(), true)) {
                this.petEntity.goalTarget = null;
                return false;
            }
        }
        if (!MyPetApi.getHookManager().canHurt(myPet.getOwner().getPlayer(), this.petEntity.goalTarget.getBukkitEntity())) {
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
        } else if (petEntity.h(petEntity.getGoalTarget()) > 400) {
            return true;
        } else if (petEntity.h(((CraftPlayer) petEntity.getOwner().getPlayer()).getHandle()) > 600) {
            return true;
        }
        return false;
    }

    @Override
    public void start() {
        petEntity.setGoalTarget(this.target, EntityTargetEvent.TargetReason.OWNER_ATTACKED_TARGET, false);
    }

    @Override
    public void finish() {
        petEntity.setGoalTarget(null, EntityTargetEvent.TargetReason.FORGOT_TARGET, false);
    }
}
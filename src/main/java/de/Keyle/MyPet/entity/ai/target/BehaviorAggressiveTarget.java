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
import net.minecraft.server.v1_7_R4.EntityLiving;
import net.minecraft.server.v1_7_R4.EntityPlayer;
import net.minecraft.server.v1_7_R4.EntityTameableAnimal;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class BehaviorAggressiveTarget extends AIGoal {
    private MyPet myPet;
    private EntityMyPet petEntity;
    private EntityPlayer petOwnerEntity;
    private EntityLiving target;
    private float range;
    private Behavior behaviorSkill = null;

    public BehaviorAggressiveTarget(EntityMyPet petEntity, float range) {
        this.petEntity = petEntity;
        this.myPet = petEntity.getMyPet();
        this.petOwnerEntity = ((CraftPlayer) myPet.getOwner().getPlayer()).getHandle();
        this.range = range;
        if (myPet.getSkills().hasSkill(Behavior.class)) {
            behaviorSkill = myPet.getSkills().getSkill(Behavior.class);
        }
    }

    @Override
    public boolean shouldStart() {
        if (behaviorSkill == null || !behaviorSkill.isActive() || behaviorSkill.getBehavior() != BehaviorState.Aggressive) {
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

        for (Object entityObj : this.petEntity.world.a(EntityLiving.class, this.petOwnerEntity.boundingBox.grow((double) range, (double) range, (double) range))) {
            EntityLiving entityLiving = (EntityLiving) entityObj;

            if (entityLiving != petEntity && entityLiving.isAlive() && petEntity.f(entityLiving) <= 91) {
                if (entityLiving instanceof EntityPlayer) {
                    Player targetPlayer = (Player) entityLiving.getBukkitEntity();
                    if (myPet.getOwner().equals(targetPlayer)) {
                        continue;
                    }
                    if (!PvPChecker.canHurt(myPet.getOwner().getPlayer(), targetPlayer)) {
                        continue;
                    }
                } else if (entityLiving instanceof EntityMyPet) {
                    MyPet targetMyPet = ((EntityMyPet) entityLiving).getMyPet();
                    if (!PvPChecker.canHurt(myPet.getOwner().getPlayer(), targetMyPet.getOwner().getPlayer())) {
                        continue;
                    }
                } else if (entityLiving instanceof EntityTameableAnimal) {
                    EntityTameableAnimal tameable = (EntityTameableAnimal) entityLiving;
                    if (tameable.isTamed() && tameable.getOwner() != null) {
                        Player tameableOwner = (Player) tameable.getOwner().getBukkitEntity();
                        if (myPet.getOwner().equals(tameableOwner)) {
                            continue;
                        } else if (!PvPChecker.canHurt(myPet.getOwner().getPlayer(), tameableOwner)) {
                            continue;
                        }
                    }
                }
                if (!PvPChecker.canHurtCitizens(entityLiving.getBukkitEntity())) {
                    continue;
                }
                this.target = entityLiving;
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
        } else if (behaviorSkill.getBehavior() != BehaviorState.Aggressive) {
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
    }

    @Override
    public void finish() {
        petEntity.setGoalTarget(null);
        target = null;
    }
}
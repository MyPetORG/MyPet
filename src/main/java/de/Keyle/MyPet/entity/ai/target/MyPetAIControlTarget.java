/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
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

import de.Keyle.MyPet.entity.ai.MyPetAIGoal;
import de.Keyle.MyPet.entity.ai.movement.MyPetAIControl;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.skill.skills.implementation.Behavior;
import de.Keyle.MyPet.skill.skills.implementation.Behavior.BehaviorState;
import de.Keyle.MyPet.util.MyPetPvP;
import net.minecraft.server.v1_5_R2.EntityLiving;
import net.minecraft.server.v1_5_R2.EntityPlayer;
import net.minecraft.server.v1_5_R2.EntityTameableAnimal;
import org.bukkit.entity.Player;

public class MyPetAIControlTarget extends MyPetAIGoal
{
    private MyPet myPet;
    private EntityMyPet petEntity;
    private EntityLiving target;
    private float range;
    private MyPetAIControl controlPathfinderGoal;

    public MyPetAIControlTarget(EntityMyPet petEntity, float range)
    {
        this.petEntity = petEntity;
        this.myPet = petEntity.getMyPet();
        this.range = range;
    }

    @Override
    public boolean shouldStart()
    {
        if (petEntity.petPathfinderSelector.hasGoal("Control"))
        {
            if (controlPathfinderGoal == null)
            {
                controlPathfinderGoal = (MyPetAIControl) petEntity.petPathfinderSelector.getGoal("Control");
            }
        }
        else
        {
            return false;
        }
        if (controlPathfinderGoal.moveTo != null && petEntity.canMove())
        {
            Behavior behaviorSkill = null;
            if (myPet.getSkills().isSkillActive("Behavior"))
            {
                behaviorSkill = (Behavior) myPet.getSkills().getSkill("Behavior");
                if (behaviorSkill.getBehavior() == Behavior.BehaviorState.Friendly)
                {
                    return false;
                }
            }
            for (Object entityObj : this.petEntity.world.a(EntityLiving.class, this.petEntity.boundingBox.grow((double) this.range, 4.0D, (double) this.range)))
            {
                EntityLiving entityLiving = (EntityLiving) entityObj;

                if (petEntity.aD().canSee(entityLiving) && entityLiving != petEntity)
                {
                    if (entityLiving instanceof EntityPlayer)
                    {
                        Player targetPlayer = (Player) entityLiving.getBukkitEntity();
                        if (myPet.getOwner().equals(targetPlayer))
                        {
                            continue;
                        }
                        else if (!MyPetPvP.canHurt(myPet.getOwner().getPlayer(), targetPlayer))
                        {
                            continue;
                        }
                    }
                    else if (entityLiving instanceof EntityTameableAnimal)
                    {
                        EntityTameableAnimal tameable = (EntityTameableAnimal) entityLiving;
                        if (tameable.isTamed() && tameable.getOwner() != null)
                        {
                            Player tameableOwner = (Player) tameable.getOwner().getBukkitEntity();
                            if (myPet.getOwner().equals(tameableOwner))
                            {
                                continue;
                            }
                            else if (!MyPetPvP.canHurt(myPet.getOwner().getPlayer(), tameableOwner))
                            {
                                continue;
                            }
                        }
                    }
                    else if (entityLiving instanceof EntityMyPet)
                    {
                        MyPet targetMyPet = ((EntityMyPet) entityLiving).getMyPet();
                        if (!MyPetPvP.canHurt(myPet.getOwner().getPlayer(), targetMyPet.getOwner().getPlayer()))
                        {
                            continue;
                        }
                    }
                    if (behaviorSkill != null)
                    {
                        if (behaviorSkill.getBehavior() == BehaviorState.Raid)
                        {
                            if (entityLiving instanceof EntityTameableAnimal)
                            {
                                continue;
                            }
                            else if (entityLiving instanceof EntityMyPet)
                            {
                                continue;
                            }
                            else if (entityLiving instanceof EntityPlayer)
                            {
                                continue;
                            }
                        }
                    }
                    controlPathfinderGoal.stopControl();
                    this.target = entityLiving;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean shouldFinish()
    {
        EntityLiving entityliving = petEntity.getGoalTarget();

        if (!petEntity.canMove())
        {
            return false;
        }
        else if (entityliving == null)
        {
            return false;
        }
        else if (!entityliving.isAlive())
        {
            return false;
        }
        return true;
    }

    @Override
    public void start()
    {
        petEntity.setGoalTarget(this.target);
    }

    @Override
    public void finish()
    {
        petEntity.setGoalTarget(null);
    }
}
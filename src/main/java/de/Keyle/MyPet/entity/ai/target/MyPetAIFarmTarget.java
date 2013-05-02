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
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.skill.skills.implementation.Behavior;
import de.Keyle.MyPet.skill.skills.implementation.Behavior.BehaviorState;
import net.minecraft.server.v1_5_R3.Entity;
import net.minecraft.server.v1_5_R3.EntityLiving;
import net.minecraft.server.v1_5_R3.EntityMonster;
import net.minecraft.server.v1_5_R3.EntityPlayer;

public class MyPetAIFarmTarget extends MyPetAIGoal
{
    private MyPet myPet;
    private EntityMyPet petEntity;
    private EntityPlayer petOwnerEntity;
    private EntityLiving target;
    private float range;
    private Behavior behaviorSkill = null;

    public MyPetAIFarmTarget(EntityMyPet petEntity, float range)
    {
        this.petEntity = petEntity;
        this.petOwnerEntity = (EntityPlayer) petEntity.getOwner();
        this.myPet = petEntity.getMyPet();
        this.range = range;
        if (myPet.getSkills().hasSkill("Behavior"))
        {
            behaviorSkill = (Behavior) myPet.getSkills().getSkill("Behavior");
        }
    }

    @Override
    public boolean shouldStart()
    {
        if (myPet.getDamage() <= 0 && myPet.getRangedDamage() <= 0)
        {
            return false;
        }
        if (behaviorSkill != null && behaviorSkill.isActive())
        {
            if (behaviorSkill.getBehavior() == BehaviorState.Farm && myPet.getCraftPet().canMove())
            {
                if (target == null || !target.isAlive())
                {
                    for (float range = 1.F ; range <= this.range ; range++)
                    {
                        for (Object entityObj : this.petEntity.world.a(EntityMonster.class, this.petOwnerEntity.boundingBox.grow((double) range, 4.0D, (double) range)))
                        {
                            Entity entity = (Entity) entityObj;
                            EntityMonster entityLiving = (EntityMonster) entity;

                            if (petEntity.getEntitySenses().canSee(entityLiving))
                            {
                                this.target = entityLiving;
                                return true;
                            }
                        }
                    }
                    return false;
                }
                return true;
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
            return true;
        }
        else if (entityliving == null)
        {
            return true;
        }
        else if (!entityliving.isAlive())
        {
            return true;
        }
        return false;
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
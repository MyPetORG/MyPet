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

import de.Keyle.MyPet.entity.ai.AIGoal;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.skill.skills.implementation.Behavior;
import de.Keyle.MyPet.skill.skills.info.BehaviorInfo.BehaviorState;
import net.minecraft.server.v1_6_R2.EntityLiving;
import net.minecraft.server.v1_6_R2.EntityMonster;
import net.minecraft.server.v1_6_R2.EntityPlayer;
import org.bukkit.craftbukkit.v1_6_R2.entity.CraftPlayer;

public class BehaviorFarmTarget extends AIGoal
{
    private MyPet myPet;
    private EntityMyPet petEntity;
    private EntityPlayer petOwnerEntity;
    private EntityLiving target;
    private float range;
    private Behavior behaviorSkill = null;

    public BehaviorFarmTarget(EntityMyPet petEntity, float range)
    {
        this.petEntity = petEntity;
        this.petOwnerEntity = ((CraftPlayer) petEntity.getOwner().getPlayer()).getHandle();
        this.myPet = petEntity.getMyPet();
        this.range = range;
        if (myPet.getSkills().hasSkill(Behavior.class))
        {
            behaviorSkill = myPet.getSkills().getSkill(Behavior.class);
        }
    }

    @Override
    public boolean shouldStart()
    {
        if (behaviorSkill == null || !behaviorSkill.isActive() || behaviorSkill.getBehavior() != BehaviorState.Farm)
        {
            return false;
        }
        if (myPet.getDamage() <= 0 && myPet.getRangedDamage() <= 0)
        {
            return false;
        }
        if (!myPet.getCraftPet().canMove())
        {
            return false;
        }
        if (petEntity.getGoalTarget() != null && petEntity.getGoalTarget().isAlive())
        {
            return false;
        }

        for (Object entityObj : this.petEntity.world.a(EntityMonster.class, this.petOwnerEntity.boundingBox.grow((double) range, (double) range, (double) range)))
        {
            EntityMonster entityMonster = (EntityMonster) entityObj;
            if (!entityMonster.isAlive() || petEntity.e(entityMonster) > 91)
            {
                continue;
            }
            this.target = entityMonster;
            return true;
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
        else if (behaviorSkill.getBehavior() != BehaviorState.Farm)
        {
            return true;
        }
        else if (myPet.getDamage() <= 0 && myPet.getRangedDamage() <= 0)
        {
            return true;
        }
        else if (petEntity.getGoalTarget().world != petEntity.world)
        {
            return true;
        }
        else if (petEntity.e(petEntity.getGoalTarget()) > 400)
        {
            return true;
        }
        else if (petEntity.e(petEntity.getOwner().getEntityPlayer()) > 600)
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
        target = null;
    }
}
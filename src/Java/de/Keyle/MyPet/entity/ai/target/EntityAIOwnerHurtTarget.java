/*
 * Copyright (C) 2011-2013 Keyle
 *
 * This file is part of MyPet
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
 * along with MyPet. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.entity.ai.target;

import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.skill.skills.Behavior;
import de.Keyle.MyPet.skill.skills.Behavior.BehaviorState;
import de.Keyle.MyPet.util.MyPetPvP;
import net.minecraft.server.v1_4_R1.EntityLiving;
import net.minecraft.server.v1_4_R1.EntityPlayer;
import net.minecraft.server.v1_4_R1.EntityTameableAnimal;
import net.minecraft.server.v1_4_R1.PathfinderGoal;
import org.bukkit.entity.Player;

public class EntityAIOwnerHurtTarget extends PathfinderGoal
{

    EntityMyPet petEntity;
    EntityLiving target;
    MyPet myPet;

    public EntityAIOwnerHurtTarget(MyPet myPet)
    {
        this.petEntity = myPet.getCraftPet().getHandle();
        this.myPet = myPet;
    }

    /**
     * Checks whether this ai goal should be activated
     */
    public boolean a()
    {
        if (!petEntity.canMove())
        {
            return false;
        }
        else if (this.petEntity.goalTarget == null)
        {
            return false;
        }
        if (myPet.getSkills().isSkillActive("Behavior"))
        {
            Behavior behaviorSkill = (Behavior) myPet.getSkills().getSkill("Behavior");
            if (behaviorSkill.getBehavior() == Behavior.BehaviorState.Friendly)
            {
                return false;
            }
            if (behaviorSkill.getBehavior() == BehaviorState.Raid)
            {
                if (this.petEntity.goalTarget instanceof EntityTameableAnimal && ((EntityTameableAnimal) this.petEntity.goalTarget).isTamed())
                {
                    return false;
                }
                if (this.petEntity.goalTarget instanceof EntityMyPet)
                {
                    return false;
                }
                if (this.petEntity.goalTarget instanceof EntityPlayer)
                {
                    return false;
                }
            }
        }
        if (this.petEntity.goalTarget instanceof EntityPlayer)
        {
            Player targetPlayer = (Player) this.petEntity.goalTarget.getBukkitEntity();

            if (myPet.getOwner().equals(targetPlayer))
            {
                return false;
            }
            else if (!MyPetPvP.canHurt(myPet.getOwner().getPlayer(), targetPlayer))
            {
                return false;
            }
        }
        else if (this.petEntity.goalTarget instanceof EntityMyPet)
        {
            MyPet targetMyPet = ((EntityMyPet) this.petEntity.goalTarget).getMyPet();
            if (!MyPetPvP.canHurt(myPet.getOwner().getPlayer(), targetMyPet.getOwner().getPlayer()))
            {
                return false;
            }
        }
        this.target = this.petEntity.goalTarget;
        this.petEntity.goalTarget = null;
        return true;
    }

    public boolean b()
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

    public void c()
    {
        petEntity.setGoalTarget(this.target);
    }

    public void d()
    {
        petEntity.setGoalTarget(null);
    }
}
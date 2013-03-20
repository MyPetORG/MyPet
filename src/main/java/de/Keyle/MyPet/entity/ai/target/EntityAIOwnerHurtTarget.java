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

import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.skill.skills.implementation.Behavior;
import de.Keyle.MyPet.skill.skills.implementation.Behavior.BehaviorState;
import de.Keyle.MyPet.util.MyPetPvP;
import net.minecraft.server.v1_5_R1.EntityLiving;
import net.minecraft.server.v1_5_R1.EntityPlayer;
import net.minecraft.server.v1_5_R1.EntityTameableAnimal;
import net.minecraft.server.v1_5_R1.PathfinderGoal;
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
                this.petEntity.goalTarget = null;
                return false;
            }
            if (behaviorSkill.getBehavior() == BehaviorState.Raid)
            {
                if (this.petEntity.goalTarget instanceof EntityTameableAnimal && ((EntityTameableAnimal) this.petEntity.goalTarget).isTamed())
                {
                    this.petEntity.goalTarget = null;
                    return false;
                }
                if (this.petEntity.goalTarget instanceof EntityMyPet)
                {
                    this.petEntity.goalTarget = null;
                    return false;
                }
                if (this.petEntity.goalTarget instanceof EntityPlayer)
                {
                    this.petEntity.goalTarget = null;
                    return false;
                }
            }
        }
        if (this.petEntity.goalTarget instanceof EntityPlayer)
        {
            Player targetPlayer = (Player) this.petEntity.goalTarget.getBukkitEntity();

            if (myPet.getOwner().equals(targetPlayer))
            {
                this.petEntity.goalTarget = null;
                return false;
            }
            else if (!MyPetPvP.canHurt(myPet.getOwner().getPlayer(), targetPlayer))
            {
                this.petEntity.goalTarget = null;
                return false;
            }
        }
        else if (this.petEntity.goalTarget instanceof EntityTameableAnimal)
        {
            EntityTameableAnimal tameable = (EntityTameableAnimal) this.petEntity.goalTarget;
            if (tameable.isTamed() && tameable.getOwner() != null)
            {
                Player tameableOwner = (Player) tameable.getOwner().getBukkitEntity();
                if (myPet.getOwner().equals(tameableOwner))
                {
                    this.petEntity.goalTarget = null;
                    return false;
                }
                else if (!MyPetPvP.canHurt(myPet.getOwner().getPlayer(), tameableOwner))
                {
                    this.petEntity.goalTarget = null;
                    return false;
                }
            }
        }
        else if (this.petEntity.goalTarget instanceof EntityMyPet)
        {
            MyPet targetMyPet = ((EntityMyPet) this.petEntity.goalTarget).getMyPet();
            if (targetMyPet == null)
            {
                this.petEntity.goalTarget = null;
                return false;
            }
            if (!MyPetPvP.canHurt(myPet.getOwner().getPlayer(), targetMyPet.getOwner().getPlayer()))
            {
                this.petEntity.goalTarget = null;
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
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

import de.Keyle.MyPet.entity.ai.movement.EntityAIControl;
import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.skill.skills.Behavior;
import de.Keyle.MyPet.skill.skills.Behavior.BehaviorState;
import de.Keyle.MyPet.util.MyPetUtil;
import net.minecraft.server.v1_4_6.EntityLiving;
import net.minecraft.server.v1_4_6.EntityPlayer;
import net.minecraft.server.v1_4_6.EntityTameableAnimal;
import net.minecraft.server.v1_4_6.PathfinderGoal;
import org.bukkit.entity.Player;

public class EntityAIControlTarget extends PathfinderGoal
{
    private MyPet myPet;
    private EntityMyPet petEntity;
    private EntityLiving target;
    private float range;
    private EntityAIControl controlPathfinderGoal;

    public EntityAIControlTarget(MyPet myPet, float range)
    {
        this.petEntity = myPet.getCraftPet().getHandle();
        this.myPet = myPet;
        this.range = range;
    }

    /**
     * Checks whether this ai should be activated
     */
    public boolean a()
    {
        if (petEntity.petPathfinderSelector.hasGoal("Control"))
        {
            if (controlPathfinderGoal == null)
            {
                controlPathfinderGoal = (EntityAIControl) petEntity.petPathfinderSelector.getGoal("Control");
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

                if (petEntity.aA().canSee(entityLiving) && entityLiving != petEntity)
                {
                    if (entityLiving instanceof EntityPlayer)
                    {
                        Player targetPlayer = (Player) entityLiving.getBukkitEntity();
                        if (myPet.getOwner().equals(targetPlayer))
                        {
                            continue;
                        }
                        if (!MyPetUtil.canHurt(myPet.getOwner().getPlayer(), targetPlayer))
                        {
                            continue;
                        }
                    }
                    else if (entityLiving instanceof EntityMyPet)
                    {
                        MyPet targetMyPet = ((EntityMyPet) entityLiving).getMyPet();
                        if (!MyPetUtil.canHurt(myPet.getOwner().getPlayer(), targetMyPet.getOwner().getPlayer()))
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
                            if (entityLiving instanceof EntityMyPet)
                            {
                                continue;
                            }
                            if (entityLiving instanceof EntityPlayer)
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

    public boolean b()
    {
        EntityLiving entityliving = petEntity.aG();

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
        petEntity.b(this.target);
    }

    public void d()
    {
        petEntity.b((EntityLiving) null);
    }
}
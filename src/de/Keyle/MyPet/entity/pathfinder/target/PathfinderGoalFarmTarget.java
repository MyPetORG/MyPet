/*
 * Copyright (C) 2011-2012 Keyle
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

package de.Keyle.MyPet.entity.pathfinder.target;

import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.skill.skills.Behavior;
import de.Keyle.MyPet.skill.skills.Behavior.BehaviorState;
import net.minecraft.server.*;
import org.bukkit.craftbukkit.entity.CraftPlayer;

public class PathfinderGoalFarmTarget extends PathfinderGoal
{
    private MyPet myPet;
    private EntityMyPet petEntity;
    private EntityPlayer petOwnerEntity;
    private EntityLiving target;
    private float range;

    public PathfinderGoalFarmTarget(MyPet myPet, float range)
    {
        this.petEntity = myPet.getPet().getHandle();
        this.petOwnerEntity = ((CraftPlayer)myPet.getOwner().getPlayer()).getHandle();
        this.myPet = myPet;
        this.range = range;
    }

    /**
     * Checks whether this pathfinder should be activated
     */
    public boolean a()
    {
        if (myPet.getSkillSystem().hasSkill("Behavior"))
        {
            Behavior behavior = (Behavior) myPet.getSkillSystem().getSkill("Behavior");
            if (behavior.getLevel() > 0)
            {
                if (behavior.getBehavior() == BehaviorState.Farm && myPet.getPet().canMove())
                {
                    if (target == null || !target.isAlive())
                    {
                        for (float range = 1.F ; range <= this.range ; range++)
                        {
                            for (Object entityObj : this.petEntity.world.a(EntityMonster.class, this.petOwnerEntity.boundingBox.grow((double) range, 4.0D, (double) range)))
                            {
                                Entity entity = (Entity) entityObj;
                                EntityMonster entityLiving = (EntityMonster) entity;

                                if (petEntity.aA().canSee(entityLiving))
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
        }
        return false;
    }

    public boolean b()
    {
        EntityLiving entityliving = petEntity.aG();

        if(!petEntity.canMove())
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
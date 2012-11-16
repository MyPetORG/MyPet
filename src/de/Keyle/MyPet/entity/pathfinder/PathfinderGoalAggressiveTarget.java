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

package de.Keyle.MyPet.entity.pathfinder;

import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.skill.skills.Behavior;
import de.Keyle.MyPet.skill.skills.Behavior.BehaviorState;
import de.Keyle.MyPet.util.MyPetUtil;
import net.minecraft.server.*;
import org.bukkit.entity.Player;

public class PathfinderGoalAggressiveTarget extends PathfinderGoalTarget
{
    private MyPet myPet;
    private EntityMyPet petEntity;
    private EntityLiving target;
    private float range;

    public PathfinderGoalAggressiveTarget(MyPet myPet, float range)
    {
        super(myPet.getPet().getHandle(), 32.0F, false);
        this.petEntity = myPet.getPet().getHandle();
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
                if (behavior.getBehavior() == Behavior.BehaviorState.Aggressive && myPet.getPet().canMove())
                {
                    if (target == null || !target.isAlive())
                    {
                        for (float range = 1.F ; range <= this.range ; range++)
                        {
                            for (Object entityObj : this.petEntity.world.a(EntityLiving.class, this.petEntity.boundingBox.grow((double) range, 4.0D, (double) range)))
                            {
                                Entity entity = (Entity) entityObj;
                                EntityLiving entityLiving = (EntityLiving) entity;

                                if (petEntity.aA().canSee(entityLiving) && entityLiving != petEntity)
                                {
                                    if(behavior.getBehavior() == BehaviorState.Farm && !(entityLiving instanceof EntityMonster))
                                    {
                                        continue;
                                    }
                                    else if (entityLiving instanceof EntityPlayer)
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

    public void c()
    {
        petEntity.b(this.target);
        super.c();
    }
}
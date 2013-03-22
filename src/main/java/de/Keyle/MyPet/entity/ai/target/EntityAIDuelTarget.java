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
import net.minecraft.server.v1_5_R2.EntityPlayer;
import net.minecraft.server.v1_5_R2.PathfinderGoal;
import org.bukkit.craftbukkit.v1_5_R2.entity.CraftPlayer;

public class EntityAIDuelTarget extends PathfinderGoal
{
    private MyPet myPet;
    private EntityMyPet petEntity;
    private EntityPlayer petOwnerEntity;
    private EntityMyPet target;
    private EntityMyPet duelOpponent = null;
    private float range;

    public EntityAIDuelTarget(MyPet myPet, float range)
    {
        this.petEntity = myPet.getCraftPet().getHandle();
        this.petOwnerEntity = ((CraftPlayer) myPet.getOwner().getPlayer()).getHandle();
        this.myPet = myPet;
        this.range = range;
    }

    /**
     * Checks whether this ai should be activated
     */
    public boolean a()
    {
        if (myPet.getDamage() == 0)
        {
            return false;
        }
        if (myPet.getSkills().isSkillActive("Behavior"))
        {
            Behavior behavior = (Behavior) myPet.getSkills().getSkill("Behavior");
            if (behavior.getBehavior() == BehaviorState.Duel && myPet.getCraftPet().canMove())
            {
                if (petEntity.getGoalTarget() == null || !petEntity.getGoalTarget().isAlive())
                {
                    if (duelOpponent != null)
                    {
                        this.target = duelOpponent;
                        return true;
                    }
                    for (float range = 1.F ; range <= this.range ; range++)
                    {
                        for (Object entityObj : this.petEntity.world.a(EntityMyPet.class, this.petOwnerEntity.boundingBox.grow((double) range, 4.0D, (double) range)))
                        {
                            EntityMyPet entityMyPet = ((EntityMyPet) entityObj);
                            MyPet targetMyPet = entityMyPet.getMyPet();

                            if (petEntity.aD().canSee(entityMyPet) && entityMyPet != petEntity && entityMyPet.isAlive())
                            {
                                if (!targetMyPet.getSkills().isSkillActive("Behavior") || !targetMyPet.getCraftPet().canMove())
                                {
                                    continue;
                                }
                                Behavior targetbehavior = (Behavior) targetMyPet.getSkills().getSkill("Behavior");
                                if (targetbehavior.getBehavior() != BehaviorState.Duel)
                                {
                                    continue;
                                }
                                if (targetMyPet.getDamage() == 0)
                                {
                                    continue;
                                }
                                this.target = entityMyPet;
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean b()
    {
        if (!petEntity.canMove())
        {
            return false;
        }
        else if (petEntity.getGoalTarget() == null)
        {
            return false;
        }
        else if (!petEntity.getGoalTarget().isAlive())
        {
            return false;
        }
        return true;
    }

    public void c()
    {
        petEntity.setGoalTarget(this.target);
        setDuelOpponent(this.target);
        if (target.petTargetSelector.hasGoal("DuelTarget"))
        {
            EntityAIDuelTarget duelGoal = (EntityAIDuelTarget) target.petTargetSelector.getGoal("DuelTarget");
            duelGoal.setDuelOpponent(this.petEntity);
        }
    }

    public void d()
    {
        petEntity.setGoalTarget(null);
        duelOpponent = null;
    }

    public EntityMyPet getDuelOpponent()
    {
        return duelOpponent;
    }

    public void setDuelOpponent(EntityMyPet opponent)
    {
        this.duelOpponent = opponent;
    }
}
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
import de.Keyle.MyPet.util.MyPetUtil;
import net.minecraft.server.EntityLiving;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.EntityTameableAnimal;
import net.minecraft.server.PathfinderGoalTarget;
import org.bukkit.entity.Player;

public class PathfinderGoalOwnerHurtTarget extends PathfinderGoalTarget
{

    EntityTameableAnimal petEntity;
    EntityLiving target;
    MyPet myPet;

    public PathfinderGoalOwnerHurtTarget(MyPet myPet)
    {
        super(myPet.getPet().getHandle(), 32.0F, false);
        this.petEntity = myPet.getPet().getHandle();
        this.myPet = myPet;
        this.a(1);
    }

    /**
     * Checks whether this pathfinder goal should be activated
     */
    public boolean a()
    {
        if (!this.petEntity.isTamed())
        {
            return false;
        }
        else
        {
            EntityLiving petOwner = this.petEntity.getOwner();

            if (petOwner == null)
            {
                return false;
            }
            else
            {
                if (myPet.getSkillSystem().hasSkill("Behavior"))
                {
                    Behavior behaviorSkill = (Behavior) myPet.getSkillSystem().getSkill("Behavior");
                    if (behaviorSkill.getLevel() > 0)
                    {
                        if (behaviorSkill.getBehavior() == Behavior.BehaviorState.Friendly)
                        {
                            return false;
                        }
                    }
                }
                this.target = ((EntityMyPet) this.petEntity).goalTarget;
                ((EntityMyPet) this.petEntity).goalTarget = null;

                if (this.target instanceof EntityPlayer)
                {
                    Player targetPlayer = (Player) this.target.getBukkitEntity();
                    if (myPet.getOwner().equals(targetPlayer))
                    {
                        this.target = null;
                        return false;
                    }
                    else if (!MyPetUtil.canHurt(myPet.getOwner().getPlayer(), targetPlayer))
                    {
                        this.target = null;
                        return false;
                    }
                }
                return true;
            }
        }
    }

    public void e()
    {
        this.d.b(this.target);
        super.e();
    }
}
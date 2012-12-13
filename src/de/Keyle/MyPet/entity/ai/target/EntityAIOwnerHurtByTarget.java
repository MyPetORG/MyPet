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

package de.Keyle.MyPet.entity.ai.target;

import de.Keyle.MyPet.entity.types.EntityMyPet;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.skill.skills.Behavior;
import de.Keyle.MyPet.skill.skills.Behavior.BehaviorState;
import de.Keyle.MyPet.util.MyPetUtil;
import net.minecraft.server.EntityLiving;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.EntityTameableAnimal;
import net.minecraft.server.PathfinderGoal;
import org.bukkit.entity.Player;

public class EntityAIOwnerHurtByTarget extends PathfinderGoal
{
    private EntityMyPet petEntity;
    private EntityLiving lastDamager;
    private MyPet myPet;

    public EntityAIOwnerHurtByTarget(EntityMyPet entityMyPet)
    {
        this.petEntity = entityMyPet;
        myPet = entityMyPet.getMyPet();
        a(1);
    }

    public boolean a()
    {
        if (!petEntity.canMove())
        {
            return false;
        }
        EntityLiving localEntityLiving = this.petEntity.getOwner();
        if (localEntityLiving == null)
        {
            return false;
        }
        this.lastDamager = localEntityLiving.aC();
        if (this.lastDamager == null || !lastDamager.isAlive())
        {
            return false;
        }
        if (lastDamager instanceof EntityPlayer)
        {
            Player targetPlayer = (Player) lastDamager.getBukkitEntity();
            if (!MyPetUtil.canHurt(myPet.getOwner().getPlayer(), targetPlayer))
            {
                return false;
            }
        }
        else if (lastDamager instanceof EntityMyPet)
        {
            MyPet targetMyPet = ((EntityMyPet) lastDamager).getMyPet();
            if (!MyPetUtil.canHurt(myPet.getOwner().getPlayer(), targetMyPet.getOwner().getPlayer()))
            {
                return false;
            }
        }
        if (myPet.getSkillSystem().hasSkill("Behavior"))
        {
            Behavior behaviorSkill = (Behavior) myPet.getSkillSystem().getSkill("Behavior");
            if (behaviorSkill.getLevel() > 0)
            {
                if (behaviorSkill.getBehavior() == Behavior.BehaviorState.Friendly)
                {
                    return false;
                }
                if (behaviorSkill.getBehavior() == BehaviorState.Raid)
                {
                    if (lastDamager instanceof EntityTameableAnimal && ((EntityTameableAnimal) lastDamager).isTamed())
                    {
                        return false;
                    }
                    if (lastDamager instanceof EntityMyPet)
                    {
                        return false;
                    }
                    if (lastDamager instanceof EntityPlayer)
                    {
                        return false;
                    }
                }
            }
        }
        return true;
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
        petEntity.b(this.lastDamager);
    }

    public void d()
    {
        petEntity.b((EntityLiving) null);
    }
}
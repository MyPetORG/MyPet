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
import de.Keyle.MyPet.util.MyPetPvP;
import net.minecraft.server.v1_4_R1.EntityLiving;
import net.minecraft.server.v1_4_R1.EntityPlayer;
import net.minecraft.server.v1_4_R1.EntityTameableAnimal;
import net.minecraft.server.v1_4_R1.PathfinderGoalTarget;
import org.bukkit.entity.Player;

public class EntityAIHurtByTarget extends PathfinderGoalTarget
{
    EntityMyPet petEntity;
    MyPet myPet;
    EntityLiving target = null;

    public EntityAIHurtByTarget(EntityMyPet petEntity)
    {
        super(petEntity, 16.0F, false);
        this.petEntity = petEntity;
        myPet = petEntity.getMyPet();
    }

    @Override
    public boolean a()
    {
        if (target != petEntity.aC())
        {
            target = petEntity.aC();
        }
        if (target instanceof EntityPlayer)
        {
            Player targetPlayer = (Player) target.getBukkitEntity();

            if (targetPlayer == myPet.getOwner().getPlayer())
            {
                return false;
            }
            else if (!MyPetPvP.canHurt(myPet.getOwner().getPlayer(), targetPlayer))
            {
                return false;
            }
        }
        else if (target instanceof EntityMyPet)
        {
            MyPet targetMyPet = ((EntityMyPet) d.aC()).getMyPet();
            if (!MyPetPvP.canHurt(myPet.getOwner().getPlayer(), targetMyPet.getOwner().getPlayer()))
            {
                return false;
            }
        }
        else if (target instanceof EntityTameableAnimal)
        {
            EntityTameableAnimal tameable = (EntityTameableAnimal) target;
            if (tameable.isTamed() && tameable.getOwner() != null)
            {
                Player tameableOwner = (Player) tameable.getOwner().getBukkitEntity();
                if (myPet.getOwner().equals(tameableOwner))
                {
                    return false;
                }
            }
        }
        return a(this.target, true);
    }

    public boolean b()
    {
        if (!petEntity.canMove())
        {
            return false;
        }
        else if (!super.b())
        {
            target = null;
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
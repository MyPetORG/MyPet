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
import de.Keyle.MyPet.util.MyPetUtil;
import net.minecraft.server.v1_4_6.EntityLiving;
import net.minecraft.server.v1_4_6.EntityPlayer;
import net.minecraft.server.v1_4_6.EntityTameableAnimal;
import net.minecraft.server.v1_4_6.PathfinderGoal;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_4_6.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class EntityAIAggressiveTarget extends PathfinderGoal
{
    private MyPet myPet;
    private EntityMyPet petEntity;
    private EntityPlayer petOwnerEntity;
    private EntityLiving target;
    private float range;

    public EntityAIAggressiveTarget(MyPet myPet, float range)
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
        if (myPet.getSkills().hasSkill("Behavior"))
        {
            Behavior behavior = (Behavior) myPet.getSkills().getSkill("Behavior");
            if (behavior.getLevel() > 0)
            {
                if (behavior.getBehavior() == BehaviorState.Aggressive && myPet.getCraftPet().canMove())
                {
                    if (petEntity.aG() == null || !petEntity.aG().isAlive())
                    {
                        for (float range = 1.F ; range <= this.range ; range++)
                        {
                            for (Object entityObj : this.petEntity.world.a(EntityLiving.class, this.petOwnerEntity.boundingBox.grow((double) range, 4.0D, (double) range)))
                            {
                                EntityLiving entityLiving = (EntityLiving) entityObj;
                                Location loc1 = entityLiving.getBukkitEntity().getLocation();
                                Location loc2 = petEntity.getBukkitEntity().getLocation();
                                if (petEntity.aA().canSee(entityLiving) && entityLiving != petEntity && entityLiving.isAlive() && MyPetUtil.getDistance2D(loc1, loc2) < 10)
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
                                    else if (entityLiving instanceof EntityTameableAnimal)
                                    {
                                        EntityTameableAnimal tameableAnimal = (EntityTameableAnimal) entityLiving;
                                        if (tameableAnimal.isTamed() && tameableAnimal.getOwner() instanceof EntityPlayer)
                                        {
                                            if (myPet.getOwner().getName().equalsIgnoreCase(tameableAnimal.getOwnerName()))
                                            {
                                                continue;
                                            }
                                            // Maybe later
                                            /*
                                            Player tamableAnimalOwner = (Player) tameableAnimal.getOwner().getBukkitEntity();
                                            if (!MyPetUtil.canHurt(myPet.getOwner().getPlayer(), tamableAnimalOwner))
                                            {
                                                continue;
                                            }
                                            */
                                        }
                                    }
                                    this.target = entityLiving;
                                    return true;
                                }
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
        else if (petEntity.aG() == null)
        {
            return false;
        }
        else if (!petEntity.aG().isAlive())
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
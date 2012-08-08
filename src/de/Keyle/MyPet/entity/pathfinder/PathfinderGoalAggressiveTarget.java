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
import net.minecraft.server.Entity;
import net.minecraft.server.EntityLiving;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.PathfinderGoalTarget;
import org.bukkit.entity.Player;

public class PathfinderGoalAggressiveTarget extends PathfinderGoalTarget
{
    private MyPet MPet;
    private EntityMyPet pet;
    private EntityLiving target;
    private float range;

    public PathfinderGoalAggressiveTarget(MyPet MPet, float range)
    {
        super(MPet.getPet().getHandle(), 32.0F, false);
        this.pet = MPet.getPet().getHandle();
        this.MPet = MPet;
        this.range = range;
    }

    public boolean a()
    {
        if (MPet.getSkillSystem().hasSkill("Behavior"))
        {
            Behavior behavior = (Behavior) MPet.getSkillSystem().getSkill("Behavior");
            if (behavior.getLevel() > 0)
            {
                if (behavior.getBehavior() == Behavior.BehaviorState.Friendly)
                {
                    return false;
                }
                else if (behavior.getBehavior() == Behavior.BehaviorState.Aggressive && !MPet.isSitting())
                {
                    if (target == null || !target.isAlive())
                    {
                        for (Object aList : this.pet.world.a(EntityLiving.class, this.pet.boundingBox.grow((double) this.range, 4.0D, (double) this.range)))
                        {
                            Entity entity = (Entity) aList;
                            EntityLiving entityliving = (EntityLiving) entity;

                            if (pet.at().canSee(entityliving) && entityliving != pet)
                            {
                                if (entityliving instanceof EntityPlayer)
                                {
                                    String playerName = ((EntityPlayer) entityliving).name;
                                    if (!MyPetUtil.getOfflinePlayer(playerName).isOnline())
                                    {
                                        continue;
                                    }
                                    Player target = MyPetUtil.getOfflinePlayer(playerName).getPlayer();

                                    if (MPet.getOwner().equals(target))
                                    {
                                        continue;
                                    }
                                    if (!MyPetUtil.canHurtFactions(MPet.getOwner().getPlayer(), target))
                                    {
                                        continue;
                                    }
                                    if (!MyPetUtil.canHurtTowny(MPet.getOwner().getPlayer(), target))
                                    {
                                        continue;
                                    }
                                    if (!MyPetUtil.canHurtWorldGuard(target))
                                    {
                                        continue;
                                    }
                                }
                                this.target = entityliving;
                                return true;
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

    public void e()
    {
        pet.b(this.target);
        super.e();
    }
}
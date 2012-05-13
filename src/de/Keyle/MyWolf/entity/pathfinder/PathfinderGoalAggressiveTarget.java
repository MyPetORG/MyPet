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

package de.Keyle.MyWolf.entity.pathfinder;

import de.Keyle.MyWolf.entity.types.wolf.EntityMyWolf;
import de.Keyle.MyWolf.entity.types.wolf.MyWolf;
import de.Keyle.MyWolf.skill.skills.Behavior;
import de.Keyle.MyWolf.util.MyWolfUtil;
import net.minecraft.server.Entity;
import net.minecraft.server.EntityLiving;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.PathfinderGoalTarget;
import org.bukkit.entity.Player;

import java.util.List;

public class PathfinderGoalAggressiveTarget extends PathfinderGoalTarget
{
    private MyWolf MWolf;
    private EntityMyWolf wolf;
    private EntityLiving target;
    private float range;

    public PathfinderGoalAggressiveTarget(MyWolf MWolf, float range)
    {
        super(MWolf.Wolf.getHandle(), 32.0F, false);
        this.wolf = MWolf.Wolf.getHandle();
        this.MWolf = MWolf;
        this.range = range;
    }

    public boolean a()
    {
        if (MWolf.SkillSystem.hasSkill("Behavior"))
        {
            Behavior behavior = (Behavior) MWolf.SkillSystem.getSkill("Behavior");
            if (behavior.getLevel() > 0)
            {
                if (behavior.getBehavior() == Behavior.BehaviorState.Friendly)
                {
                    return false;
                }
                else if (behavior.getBehavior() == Behavior.BehaviorState.Aggressive && !MWolf.isSitting())
                {
                    if (target == null || !target.isAlive())
                    {
                        List list = this.wolf.world.a(EntityLiving.class, this.wolf.boundingBox.grow((double) this.range, 4.0D, (double) this.range));

                        for (Object aList : list)
                        {
                            Entity entity = (Entity) aList;
                            EntityLiving entityliving = (EntityLiving) entity;

                            if (wolf.am().canSee(entityliving) && entityliving != wolf)
                            {
                                if (entityliving instanceof EntityPlayer)
                                {
                                    String playerName = ((EntityPlayer) entityliving).name;
                                    if (!MyWolfUtil.getOfflinePlayer(playerName).isOnline())
                                    {
                                        continue;
                                    }
                                    Player target = MyWolfUtil.getOfflinePlayer(playerName).getPlayer();

                                    if (target == MWolf.getOwner())
                                    {
                                        continue;
                                    }
                                    if (!MyWolfUtil.canHurtFactions(MWolf.getOwner().getPlayer(), target))
                                    {
                                        continue;
                                    }
                                    if (!MyWolfUtil.canHurtTowny(MWolf.getOwner().getPlayer(), target))
                                    {
                                        continue;
                                    }
                                    if (!MyWolfUtil.canHurtWorldGuard(target))
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

    public void c()
    {
        wolf.b(this.target);
        super.c();
    }
}
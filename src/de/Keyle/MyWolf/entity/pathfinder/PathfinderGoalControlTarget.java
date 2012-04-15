/*
 * Copyright (C) 2011-2012 Keyle
 *
 * This file is part of MyWolf
 *
 * MyWolf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyWolf is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyWolf. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyWolf.entity.pathfinder;

import de.Keyle.MyWolf.MyWolf;
import de.Keyle.MyWolf.entity.EntityMyWolf;
import de.Keyle.MyWolf.skill.skills.Behavior;
import de.Keyle.MyWolf.util.MyWolfUtil;
import net.minecraft.server.Entity;
import net.minecraft.server.EntityHuman;
import net.minecraft.server.EntityLiving;
import net.minecraft.server.PathfinderGoalTarget;
import org.bukkit.entity.Player;

import java.util.List;

public class PathfinderGoalControlTarget extends PathfinderGoalTarget
{
    private MyWolf MWolf;
    private EntityMyWolf wolf;
    private EntityLiving target;
    private float range;
    private PathfinderGoalControl control;

    public PathfinderGoalControlTarget(MyWolf MWolf, PathfinderGoalControl control, float range)
    {
        super(MWolf.Wolf.getHandle(), 32.0F, false);
        this.wolf = MWolf.Wolf.getHandle();
        this.MWolf = MWolf;
        this.range = range;
        this.control = control;
    }

    public boolean a()
    {
        if (control.moveTo != null && !wolf.isSitting())
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
                }
            }
            List list = this.wolf.world.a(EntityLiving.class, this.wolf.boundingBox.grow((double) this.range, 4.0D, (double) this.range));

            for (Object aList : list)
            {
                Entity entity = (Entity) aList;
                EntityLiving entityliving = (EntityLiving) entity;

                if (wolf.am().canSee(entityliving) && entityliving != wolf && !(entityliving instanceof EntityHuman && ((EntityHuman) entityliving).name.equals(MWolf.getOwner().getName())))
                {
                    if (entityliving instanceof EntityHuman)
                    {
                        if(!MyWolfUtil.canHurtFaction(MWolf.getOwner().getPlayer(), ((Player) ((EntityHuman) entityliving).getBukkitEntity())))
                        {
                            continue;
                        }
                        if(!MyWolfUtil.canHurtTowny(MWolf.getOwner().getPlayer(), ((Player) ((EntityHuman) entityliving).getBukkitEntity())))
                        {
                            continue;
                        }
                        if(!MyWolfUtil.getPVP(((EntityHuman) entityliving).getBukkitEntity().getLocation()))
                        {
                            continue;
                        }
                    }
                    this.target = entityliving;
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

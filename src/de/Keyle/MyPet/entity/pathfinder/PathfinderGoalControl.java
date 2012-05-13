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

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.entity.types.wolf.MyWolf;
import de.Keyle.MyPet.skill.skills.Control;
import de.Keyle.MyPet.util.MyPetUtil;
import de.Keyle.MyPet.util.Scheduler;
import net.minecraft.server.Navigation;
import net.minecraft.server.PathfinderGoal;
import org.bukkit.Location;

public class PathfinderGoalControl extends PathfinderGoal implements Scheduler
{
    private MyWolf MPet;
    private float speed;
    Location moveTo = null;
    private int TimeToMove = 0;
    private Navigation nav;

    public PathfinderGoalControl(MyWolf MPet, float f)
    {
        this.MPet = MPet;
        speed = f;
        nav = this.MPet.Wolf.getHandle().al();
    }

    @Override
    public boolean a()
    {
        if (MPet.getSkillSystem().hasSkill("Control") && MPet.getSkillSystem().getSkill("Control").getLevel() > 0)
        {
            Control control = (Control) MPet.getSkillSystem().getSkill("Control");
            if (control.getLocation(false) != null)
            {
                moveTo = control.getLocation();
                TimeToMove = (int) MyPetUtil.getDistance(MPet.getLocation(), moveTo) / 3;
            }
        }
        return moveTo != null;
    }

    public void c()
    {
        if (nav.a(this.moveTo.getX(), this.moveTo.getY(), this.moveTo.getZ(), this.speed))
        {
            MyPetPlugin.getPlugin().getTimer().addTask(this);
        }
        else
        {
            moveTo = null;
        }
    }

    public boolean b()
    {
        Control control = (Control) MPet.getSkillSystem().getSkill("Control");
        if (control.getLocation(false) != null)
        {
            moveTo = control.getLocation();
            TimeToMove = (int) MyPetUtil.getDistance(MPet.getLocation(), moveTo) / 3;
            if (!nav.a(this.moveTo.getX(), this.moveTo.getY(), this.moveTo.getZ(), this.speed))
            {
                moveTo = null;
            }
        }
        return moveTo != null && !this.MPet.Wolf.isSitting();
    }

    public void d()
    {
        MyPetPlugin.getPlugin().getTimer().removeTask(this);
        moveTo = null;
    }

    public void schedule()
    {
        TimeToMove--;
        if (TimeToMove <= 0)
        {
            moveTo = null;
        }
    }
}
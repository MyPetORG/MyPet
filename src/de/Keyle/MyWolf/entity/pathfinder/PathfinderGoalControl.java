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
import de.Keyle.MyWolf.MyWolfPlugin;
import de.Keyle.MyWolf.skill.skills.Control;
import de.Keyle.MyWolf.util.MyWolfUtil;
import de.Keyle.MyWolf.util.Scheduler;
import net.minecraft.server.Navigation;
import net.minecraft.server.PathfinderGoal;
import org.bukkit.Location;

public class PathfinderGoalControl extends PathfinderGoal implements Scheduler
{
    private MyWolf MWolf;
    private float speed;
    Location moveTo = null;
    private int TimeToMove = 0;
    private Navigation nav;

    public PathfinderGoalControl(MyWolf MWolf, float f)
    {
        this.MWolf = MWolf;
        speed = f;
        nav = this.MWolf.Wolf.getHandle().al();
    }

    @Override
    public boolean a()
    {
        if (MWolf.SkillSystem.hasSkill("Control") && MWolf.SkillSystem.getSkill("Control").getLevel() > 0)
        {
            Control control = (Control) MWolf.SkillSystem.getSkill("Control");
            if (control.getLocation(false) != null)
            {
                moveTo = control.getLocation();
                TimeToMove = (int) MyWolfUtil.getDistance(MWolf.getLocation(), moveTo) / 3;
            }
        }
        return moveTo != null;
    }

    public void c()
    {
        if (nav.a(this.moveTo.getX(), this.moveTo.getY(), this.moveTo.getZ(), this.speed))
        {
            MyWolfPlugin.getPlugin().getTimer().addTask(this);
        }
        else
        {
            moveTo = null;
        }
    }

    public boolean b()
    {
        Control control = (Control) MWolf.SkillSystem.getSkill("Control");
        if (control.getLocation(false) != null)
        {
            moveTo = control.getLocation();
            TimeToMove = (int) MyWolfUtil.getDistance(MWolf.getLocation(), moveTo) / 3;
            if (!nav.a(this.moveTo.getX(), this.moveTo.getY(), this.moveTo.getZ(), this.speed))
            {
                moveTo = null;
            }
        }
        return moveTo != null && !this.MWolf.Wolf.isSitting();
    }

    public void d()
    {
        MyWolfPlugin.getPlugin().getTimer().removeTask(this);
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
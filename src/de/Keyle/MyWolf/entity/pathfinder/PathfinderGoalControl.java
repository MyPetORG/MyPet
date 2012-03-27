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
import net.minecraft.server.PathfinderGoal;
import org.bukkit.Location;

public class PathfinderGoalControl extends PathfinderGoal implements Scheduler
{
    MyWolf MWolf;
    float Speed;
    Location moveTo = null;
    int TimeToMove = 0;

    public PathfinderGoalControl(MyWolf MWolf, float f)
    {
        this.MWolf = MWolf;
        Speed = f;
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
                TimeToMove = (int) MyWolfUtil.getDistance(MWolf.getLocation(), moveTo);
                MyWolfUtil.getLogger().info("--- distanz: " + TimeToMove + " ---");
            }
        }
        return moveTo != null;
    }

    public void c()
    {
        MyWolfUtil.getLogger().info("--- active ---");
        if(this.MWolf.Wolf.getHandle().al().a(this.moveTo.getX(), this.moveTo.getY(), this.moveTo.getZ(), this.Speed))
        {
            this.MWolf.Wolf.getHandle().al().a(false);
            MyWolfPlugin.getPlugin().getTimer().addTask(this);
        }
    }

    public void d()
    {
        MyWolfPlugin.getPlugin().getTimer().removeTask(this);
        MyWolfUtil.getLogger().info("--- stopped ---");
    }

    public void schedule()
    {
        TimeToMove--;
        if(TimeToMove <= 0)
        {
            moveTo = null;
        }
    }
}

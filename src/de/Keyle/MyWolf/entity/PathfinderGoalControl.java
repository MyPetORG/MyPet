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

package de.Keyle.MyWolf.entity;

import de.Keyle.MyWolf.MyWolf;
import de.Keyle.MyWolf.skill.skills.Control;
import net.minecraft.server.PathfinderGoal;
import org.bukkit.Location;

/**
 * Created by IntelliJ IDEA.
 * User: Keyle
 * Date: 11.03.12
 * Time: 19:11
 * To change this template use File | Settings | File Templates.
 */
public class PathfinderGoalControl extends PathfinderGoal
{
    MyWolf MWolf;
    float Speed;
    Location moveTo;
    
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
            moveTo = control.getLocation();
            if(moveTo != null)
            {
                return true;
            }
        }
        return false;
    }

    public void c()
    {
        if(moveTo != null)
        {
            this.MWolf.Wolf.getHandle().ak().a(this.moveTo.getX(), this.moveTo.getY(), this.moveTo.getZ(), this.Speed);
            moveTo = null;
        }
    }
}

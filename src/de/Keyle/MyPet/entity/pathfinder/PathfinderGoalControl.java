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
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.skill.skills.Control;
import de.Keyle.MyPet.util.MyPetUtil;
import de.Keyle.MyPet.util.Scheduler;
import net.minecraft.server.Navigation;
import net.minecraft.server.PathfinderGoal;
import org.bukkit.Location;

public class PathfinderGoalControl extends PathfinderGoal implements Scheduler
{
    private MyPet myPet;
    private float speed;
    Location moveTo = null;
    private int timeToMove = 0;
    private Navigation nav;

    public PathfinderGoalControl(MyPet myPet, float f)
    {
        this.myPet = myPet;
        speed = f;
        nav = this.myPet.getPet().getHandle().getNavigation();
    }

    /**
     * Checks whether this pathfinder should be activated
     */
    public boolean a()
    {
        MyPetUtil.getLogger().info("a");
        if (myPet.getSkillSystem().hasSkill("Control") && myPet.getSkillSystem().getSkill("Control").getLevel() > 0)
        {
            Control controlSkill = (Control) myPet.getSkillSystem().getSkill("Control");
            if (controlSkill.getLocation(false) != null)
            {
                moveTo = controlSkill.getLocation();
                timeToMove = (int) MyPetUtil.getDistance2D(myPet.getLocation(), moveTo) / 3;
            }
        }
        return moveTo != null;
    }

    /**
     * This method is called when this pathfinder is activated
     */
    public void e()
    {
        MyPetUtil.getLogger().info("e");
        //nav.a -> move to the location (x,y,z) with given speed
        if (nav.a(this.moveTo.getX(), this.moveTo.getY(), this.moveTo.getZ(), this.speed))
        {
            MyPetPlugin.getPlugin().getTimer().addTask(this);
        }
        else
        {
            moveTo = null;
        }
    }

    /**
     * Checks whether this pathfinder should be stopped
     */
    public boolean b()
    {
        MyPetUtil.getLogger().info("b");
        Control control = (Control) myPet.getSkillSystem().getSkill("Control");
        if (control.getLocation(false) != null)
        {
            moveTo = control.getLocation();
            timeToMove = (int) MyPetUtil.getDistance2D(myPet.getLocation(), moveTo) / 3;
            if (!nav.a(this.moveTo.getX(), this.moveTo.getY(), this.moveTo.getZ(), this.speed))
            {
                moveTo = null;
            }
        }
        return moveTo != null && !this.myPet.isSitting();
    }

    /**
     * This method is called when this pathfinder is stopped
     */
    public void d()
    {
        MyPetUtil.getLogger().info("d");
        MyPetPlugin.getPlugin().getTimer().removeTask(this);
        moveTo = null;
    }

    public void schedule()
    {
        timeToMove--;
        if (timeToMove <= 0)
        {
            moveTo = null;
        }
    }
}
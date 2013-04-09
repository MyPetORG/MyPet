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

package de.Keyle.MyPet.entity.ai.movement;

import de.Keyle.MyPet.entity.ai.EntityAIGoal;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.skill.skills.implementation.Control;
import de.Keyle.MyPet.util.IScheduler;
import net.minecraft.server.v1_5_R2.Navigation;
import org.bukkit.Location;

public class EntityAIControl extends EntityAIGoal implements IScheduler
{
    private MyPet myPet;
    private float speed;
    public Location moveTo = null;
    private int timeToMove = 0;
    private Navigation nav;
    private boolean stopControl = false;
    private Control controlSkill;

    public EntityAIControl(MyPet myPet, float speed)
    {
        this.myPet = myPet;
        this.speed = speed;
        nav = this.myPet.getCraftPet().getHandle().getNavigation();
        controlSkill = (Control) myPet.getSkills().getSkill("Control");
    }

    /**
     * Checks whether this ai should be activated
     */
    public boolean shouldStart()
    {
        if (stopControl)
        {
            stopControl = false;
        }
        if (myPet.getSkills().isSkillActive("Control"))
        {
            return controlSkill.getLocation(false) != null;
        }
        return false;
    }

    /**
     * Checks whether this ai should be stopped
     */
    @Override
    public boolean shouldFinish()
    {
        boolean stop = false;

        if (controlSkill.getLocation(false) != null && moveTo != controlSkill.getLocation(false))
        {
            moveTo = controlSkill.getLocation();
            timeToMove = (int) myPet.getLocation().distance(moveTo) / 3;
            timeToMove = timeToMove < 3 ? 3 : timeToMove;
            if (!nav.a(this.moveTo.getX(), this.moveTo.getY(), this.moveTo.getZ(), this.speed))
            {
                moveTo = null;
                stop = true;
                stopControl = false;
            }
        }

        if (!this.myPet.getCraftPet().canMove() || moveTo != null && myPet.getLocation().distance(moveTo) < 1 || timeToMove <= 0 || moveTo == null || stopControl)
        {
            moveTo = null;
            stop = true;
            stopControl = false;
        }
        return !stop;
    }

    public void stopControl()
    {
        this.stopControl = true;
    }

    @Override
    public void schedule()
    {
        timeToMove--;
    }
}
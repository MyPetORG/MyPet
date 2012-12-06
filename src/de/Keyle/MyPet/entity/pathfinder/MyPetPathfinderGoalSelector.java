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

import net.minecraft.server.PathfinderGoal;
import net.minecraft.server.PathfinderGoalSelector;

import java.util.HashMap;
import java.util.Map;

public class MyPetPathfinderGoalSelector
{
    private PathfinderGoalSelector goalSelector;
    private Map<String, PathfinderGoal> goalMap = new HashMap<String, PathfinderGoal>();
    private int goalPos = 1;

    public MyPetPathfinderGoalSelector(PathfinderGoalSelector goalSelector)
    {
        this.goalSelector = goalSelector;
    }

    public void addGoal(String name, PathfinderGoal goalSelector)
    {
        if (goalMap.containsKey(name))
        {
            return;
        }
        goalMap.put(name, goalSelector);
        this.goalSelector.a(goalPos, goalSelector);
        goalPos++;
    }

    public void addGoal(String name, int pos, PathfinderGoal goalSelector)
    {
        if (goalMap.containsKey(name))
        {
            return;
        }
        goalMap.put(name, goalSelector);
        this.goalSelector.a(pos, goalSelector);
    }

    public void addGoal(String name, boolean increment, PathfinderGoal goalSelector)
    {
        if (goalMap.containsKey(name))
        {
            return;
        }
        goalMap.put(name, goalSelector);
        this.goalSelector.a(goalPos, goalSelector);
        if (increment)
        {
            goalPos++;
        }
    }

    public boolean hasGoal(String name)
    {
        return goalMap.containsKey(name);
    }

    public PathfinderGoal getGoal(String name)
    {
        return goalMap.get(name);
    }
}

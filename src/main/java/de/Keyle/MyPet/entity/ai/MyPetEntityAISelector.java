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

package de.Keyle.MyPet.entity.ai;

import net.minecraft.server.v1_5_R2.PathfinderGoal;
import net.minecraft.server.v1_5_R2.PathfinderGoalSelector;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyPetEntityAISelector
{
    private PathfinderGoalSelector entityAISelector;
    private Map<String, PathfinderGoal> AIGoalMap = new HashMap<String, PathfinderGoal>();
    private int goalPos = 0;

    public MyPetEntityAISelector(PathfinderGoalSelector entityAISelector)
    {
        this.entityAISelector = entityAISelector;
    }

    public void addGoal(String name, PathfinderGoal entityAIgoal)
    {
        if (AIGoalMap.containsKey(name))
        {
            return;
        }
        AIGoalMap.put(name, entityAIgoal);
        this.entityAISelector.a(goalPos, entityAIgoal);
        goalPos++;
    }

    public void addGoal(String name, int pos, PathfinderGoal goalSelector)
    {
        if (AIGoalMap.containsKey(name))
        {
            return;
        }
        AIGoalMap.put(name, goalSelector);
        this.entityAISelector.a(pos, goalSelector);
    }

    public void addGoal(String name, boolean increment, PathfinderGoal goalSelector)
    {
        if (AIGoalMap.containsKey(name))
        {
            return;
        }
        AIGoalMap.put(name, goalSelector);
        this.entityAISelector.a(goalPos, goalSelector);
        if (increment)
        {
            goalPos++;
        }
    }

    public boolean hasGoal(String name)
    {
        return AIGoalMap.containsKey(name);
    }

    public PathfinderGoal getGoal(String name)
    {
        return AIGoalMap.get(name);
    }

    public boolean clearGoals()
    {
        try
        {
            Field goalSelector_a = entityAISelector.getClass().getDeclaredField("a");
            goalSelector_a.setAccessible(true);
            if (goalSelector_a.get(this.entityAISelector) instanceof List)
            {
                ((List) goalSelector_a.get(this.entityAISelector)).clear();
                AIGoalMap.clear();
                goalPos = 1;
                return true;
            }
        }
        catch (Exception e)
        {
            return false;
        }
        return false;
    }
}
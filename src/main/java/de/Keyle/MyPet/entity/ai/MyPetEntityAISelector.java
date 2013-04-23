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

import java.util.*;

public class MyPetEntityAISelector
{
    private Map<String, EntityAIGoal> AIGoalMap = new HashMap<String, EntityAIGoal>();
    private List<EntityAIGoal> AIGoalList = new LinkedList<EntityAIGoal>();
    private List<EntityAIGoal> activeAIGoalList = new LinkedList<EntityAIGoal>();

    public void addGoal(String name, EntityAIGoal entityAIgoal)
    {
        if (AIGoalMap.containsKey(name))
        {
            return;
        }
        AIGoalMap.put(name, entityAIgoal);
        AIGoalList.add(entityAIgoal);
    }

    public void addGoal(String name, int pos, EntityAIGoal entityAIgoal)
    {
        if (AIGoalMap.containsKey(name))
        {
            return;
        }
        AIGoalMap.put(name, entityAIgoal);
        AIGoalList.add(pos, entityAIgoal);
    }

    public void replaceGoal(String name, EntityAIGoal entityAIgoal)
    {
        if (AIGoalMap.containsKey(name))
        {
            EntityAIGoal oldGoal = AIGoalMap.get(name);
            int index = AIGoalList.indexOf(oldGoal);
            AIGoalList.add(index, entityAIgoal);
            AIGoalList.remove(oldGoal);
            AIGoalMap.put(name, entityAIgoal);
        }
        else
        {
            addGoal(name, entityAIgoal);
        }
    }

    public void removeGoal(String name)
    {
        if (AIGoalMap.containsKey(name))
        {
            EntityAIGoal goal = AIGoalMap.get(name);
            AIGoalList.remove(goal);
            AIGoalMap.remove(name);
        }
    }

    public boolean hasGoal(String name)
    {
        return AIGoalMap.containsKey(name);
    }

    public EntityAIGoal getGoal(String name)
    {
        return AIGoalMap.get(name);
    }

    public void clearGoals()
    {
        AIGoalList.clear();
        AIGoalMap.clear();
    }

    public void tick()
    {
        // add goals
        ListIterator iterator = AIGoalList.listIterator();
        while (iterator.hasNext())
        {
            EntityAIGoal goal = (EntityAIGoal) iterator.next();
            if (!activeAIGoalList.contains(goal))
            {
                if (goal.shouldStart())
                {
                    goal.start();
                    activeAIGoalList.add(goal);
                }
            }
        }

        // remove goals
        iterator = activeAIGoalList.listIterator();
        while (iterator.hasNext())
        {
            EntityAIGoal goal = (EntityAIGoal) iterator.next();
            if (!goal.shouldFinish())
            {
                goal.finish();
                iterator.remove();
            }
        }

        // tick goals
        iterator = activeAIGoalList.listIterator();
        while (iterator.hasNext())
        {
            EntityAIGoal goal = (EntityAIGoal) iterator.next();
            goal.tick();
        }
    }
}
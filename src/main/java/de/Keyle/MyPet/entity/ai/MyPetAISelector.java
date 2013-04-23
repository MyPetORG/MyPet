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

public class MyPetAISelector
{
    private Map<String, MyPetAIGoal> AIGoalMap = new HashMap<String, MyPetAIGoal>();
    private List<MyPetAIGoal> AIGoalList = new LinkedList<MyPetAIGoal>();
    private List<MyPetAIGoal> activeAIGoalList = new LinkedList<MyPetAIGoal>();

    public void addGoal(String name, MyPetAIGoal myPetAIgoal)
    {
        if (AIGoalMap.containsKey(name))
        {
            return;
        }
        AIGoalMap.put(name, myPetAIgoal);
        AIGoalList.add(myPetAIgoal);
    }

    public void addGoal(String name, int pos, MyPetAIGoal myPetAIgoal)
    {
        if (AIGoalMap.containsKey(name))
        {
            return;
        }
        AIGoalMap.put(name, myPetAIgoal);
        AIGoalList.add(pos, myPetAIgoal);
    }

    public void replaceGoal(String name, MyPetAIGoal myPetAIgoal)
    {
        if (AIGoalMap.containsKey(name))
        {
            MyPetAIGoal oldGoal = AIGoalMap.get(name);
            int index = AIGoalList.indexOf(oldGoal);
            AIGoalList.add(index, myPetAIgoal);
            AIGoalList.remove(oldGoal);
            AIGoalMap.put(name, myPetAIgoal);
        }
        else
        {
            addGoal(name, myPetAIgoal);
        }
    }

    public void removeGoal(String name)
    {
        if (AIGoalMap.containsKey(name))
        {
            MyPetAIGoal goal = AIGoalMap.get(name);
            AIGoalList.remove(goal);
            AIGoalMap.remove(name);
        }
    }

    public boolean hasGoal(String name)
    {
        return AIGoalMap.containsKey(name);
    }

    public MyPetAIGoal getGoal(String name)
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
            MyPetAIGoal goal = (MyPetAIGoal) iterator.next();
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
            MyPetAIGoal goal = (MyPetAIGoal) iterator.next();
            if (goal.shouldFinish())
            {
                goal.finish();
                iterator.remove();
            }
        }

        // tick goals
        iterator = activeAIGoalList.listIterator();
        while (iterator.hasNext())
        {
            MyPetAIGoal goal = (MyPetAIGoal) iterator.next();
            goal.tick();
        }
    }
}
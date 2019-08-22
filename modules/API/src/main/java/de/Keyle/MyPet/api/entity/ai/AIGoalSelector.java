/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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

package de.Keyle.MyPet.api.entity.ai;

import java.util.*;

public class AIGoalSelector {

    private Map<String, AIGoal> AIGoalMap = new HashMap<>();
    private List<AIGoal> AIGoalList = new LinkedList<>();
    private List<AIGoal> activeAIGoalList = new LinkedList<>();

    int skippedTicks = 0;
    private int skipTicks;

    public AIGoalSelector(int skipTicks) {
        this.skipTicks = skipTicks;
    }

    public void addGoal(String name, AIGoal myPetAIgoal) {
        if (AIGoalMap.containsKey(name)) {
            return;
        }
        AIGoalMap.put(name, myPetAIgoal);
        AIGoalList.add(myPetAIgoal);
    }

    public void addGoal(String name, int pos, AIGoal myPetAIgoal) {
        if (AIGoalMap.containsKey(name)) {
            return;
        }
        AIGoalMap.put(name, myPetAIgoal);
        AIGoalList.add(pos, myPetAIgoal);
    }

    public void replaceGoal(String name, AIGoal myPetAIgoal) {
        if (AIGoalMap.containsKey(name)) {
            AIGoal oldGoal = AIGoalMap.get(name);
            if (activeAIGoalList.contains(oldGoal)) {
                activeAIGoalList.remove(oldGoal);
                oldGoal.finish();
            }
            int index = AIGoalList.indexOf(oldGoal);
            AIGoalList.add(index, myPetAIgoal);
            AIGoalList.remove(oldGoal);
            AIGoalMap.put(name, myPetAIgoal);
        } else {
            addGoal(name, myPetAIgoal);
        }
    }

    public void removeGoal(String name) {
        if (AIGoalMap.containsKey(name)) {
            AIGoal goal = AIGoalMap.get(name);
            AIGoalList.remove(goal);
            AIGoalMap.remove(name);
            if (activeAIGoalList.contains(goal)) {
                goal.finish();
            }
            activeAIGoalList.remove(goal);
        }
    }

    public boolean hasGoal(String name) {
        return AIGoalMap.containsKey(name);
    }

    public AIGoal getGoal(String name) {
        return AIGoalMap.get(name);
    }

    public void clearGoals() {
        AIGoalList.clear();
        AIGoalMap.clear();
        for (AIGoal goal : activeAIGoalList) {
            goal.finish();
        }
        activeAIGoalList.clear();
    }

    public void finish() {
        for (AIGoal goal : activeAIGoalList) {
            goal.finish();
        }
        activeAIGoalList.clear();
    }

    public void tick() {
        if (skipTicks > 0) {
            if (skippedTicks++ < skipTicks) {
                return;
            } else {
                skippedTicks = 0;
            }
        }

        // add goals
        ListIterator iterator = AIGoalList.listIterator();
        while (iterator.hasNext()) {
            AIGoal goal = (AIGoal) iterator.next();
            if (!activeAIGoalList.contains(goal)) {
                if (goal.shouldStart()) {
                    goal.start();
                    activeAIGoalList.add(goal);
                }
            }
        }

        // remove goals
        iterator = activeAIGoalList.listIterator();
        while (iterator.hasNext()) {
            AIGoal goal = (AIGoal) iterator.next();
            if (goal.shouldFinish()) {
                goal.finish();
                iterator.remove();
            }
        }

        // tick goals
        iterator = activeAIGoalList.listIterator();
        while (iterator.hasNext()) {
            AIGoal goal = (AIGoal) iterator.next();
            goal.tick();
        }
    }
}
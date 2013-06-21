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

package de.Keyle.MyPet.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyPetWorldGroup
{
    private static List<MyPetWorldGroup> allGroups = new ArrayList<MyPetWorldGroup>();
    private static Map<String, MyPetWorldGroup> groupWorlds = new HashMap<String, MyPetWorldGroup>();

    private String name;
    private List<String> worlds = new ArrayList<String>();


    public MyPetWorldGroup(String groupName)
    {
        this.name = groupName.toLowerCase();
    }

    public void registerGroup()
    {
        for (MyPetWorldGroup group : getGroups())
        {
            if (group.getName().equalsIgnoreCase(name))
            {
                return;
            }
        }
        allGroups.add(this);
    }

    public boolean addWorld(String world)
    {
        for (MyPetWorldGroup group : getGroups())
        {
            if (group.containsWorld(world))
            {
                return false;
            }
        }
        if (!this.worlds.contains(world))
        {
            this.worlds.add(world);
            groupWorlds.put(world, this);
            return true;
        }
        return false;
    }

    public String getName()
    {
        return this.name;
    }

    public List<String> getWorlds()
    {
        return this.worlds;
    }

    /**
     * Checks whether a world group contains the world
     *
     * @param worldName The name of the checked world
     * @return boolean
     */
    public boolean containsWorld(String worldName)
    {
        return this.worlds.contains(worldName);
    }

    /**
     * Returns all available world groups
     *
     * @return MyPetWorldGroup[]
     */
    public static MyPetWorldGroup[] getGroups()
    {
        MyPetWorldGroup[] groups = new MyPetWorldGroup[allGroups.size()];
        for (int i = 0 ; i < allGroups.size() ; i++)
        {
            groups[i] = allGroups.get(i);
        }
        return groups;
    }

    /**
     * Returns the group the world is in
     *
     * @param name World
     * @return MyPetWorldGroup
     */
    public static MyPetWorldGroup getGroup(String name)
    {
        return groupWorlds.get(name);
    }

    /**
     * Removes all worlds from the groups and then deletes the groups
     */
    public static void clearGroups()
    {
        allGroups.clear();
        groupWorlds.clear();
    }
}
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

package de.Keyle.MyWolf.skill;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyWolfSkillTree implements Cloneable
{
    String Name;

    Map<Integer, List<String>> SkillsPerLevel = new HashMap<Integer, List<String>>();

    public MyWolfSkillTree(String Name)
    {
        this.Name = Name;
    }

    public String getName()
    {
        return Name;
    }

    public void addLevel(int Level, String Skill)
    {
        if (!SkillsPerLevel.containsKey(Level))
        {
            List<String> stringList = new ArrayList<String>();
            stringList.add((Skill));
            SkillsPerLevel.put(Level, stringList);
        }
        else
        {
            addSkillToLevel(Level, Skill);
        }
    }

    public void addLevel(int Level, List<String> Skills)
    {
        SkillsPerLevel.put(Level, Skills);
    }

    public void addSkillToLevel(int Level, String Skill)
    {
        if (SkillsPerLevel.containsKey(Level))
        {
            SkillsPerLevel.get(Level).add(Skill);
        }
        else
        {
            List<String> tmps = new ArrayList<String>();
            tmps.add(Skill);
            addLevel(Level, tmps);
        }
    }

    public void addSkillToLevel(int Level, List<String> Skills)
    {
        if (SkillsPerLevel.containsKey(Level))
        {
            SkillsPerLevel.get(Level).addAll(Skills);
        }
        else
        {
            addLevel(Level, Skills);
        }
    }

    public void addSkillToLevel(int Level, String[] Skills)
    {
        if (SkillsPerLevel.containsKey(Level))
        {
            for (String skillname : Skills)
            {
                SkillsPerLevel.get(Level).add(skillname);
            }
        }
        else
        {
            for (String skillname : Skills)
            {
                addSkillToLevel(Level, skillname);
            }
        }
    }

    public Integer[] getLevels()
    {
        if (SkillsPerLevel.size() > 0)
        {
            Integer[] Levels = new Integer[SkillsPerLevel.keySet().size()];
            int i = 0;
            for (int level : SkillsPerLevel.keySet())
            {
                Levels[i] = level;
                i++;
            }
            return Levels;
        }
        return new Integer[0];
    }

    public String[] getSkills(int Level)
    {
        if (SkillsPerLevel.containsKey(Level) && SkillsPerLevel.get(Level).size() > 0)
        {
            String[] SN = new String[SkillsPerLevel.get(Level).size()];
            for (int i = 0; i < SkillsPerLevel.get(Level).size(); i++)
            {
                SN[i] = SkillsPerLevel.get(Level).get(i);
            }
            return SN;
        }
        return new String[0];
    }

    public Object cloneSkillTree()
    {
        try
        {
            return super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            return null;
        }
    }
}

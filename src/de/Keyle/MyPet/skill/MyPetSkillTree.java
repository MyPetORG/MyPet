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

package de.Keyle.MyPet.skill;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class MyPetSkillTree
{
    private String Name;

    private SortedMap<Integer, List<MyPetSkillTreeSkill>> SkillsPerLevel = new TreeMap<Integer, List<MyPetSkillTreeSkill>>();

    public MyPetSkillTree(String Name)
    {
        this.Name = Name;
    }

    public String getName()
    {
        return Name;
    }

    public void addLevel(int Level, MyPetSkillTreeSkill skill)
    {
        if (!SkillsPerLevel.containsKey(Level))
        {
            List<MyPetSkillTreeSkill> skillList = new ArrayList<MyPetSkillTreeSkill>();
            skillList.add((skill));
            SkillsPerLevel.put(Level, skillList);
        }
        else
        {
            addSkillToLevel(Level, skill);
        }
    }

    public void addLevel(int Level, List<MyPetSkillTreeSkill> skills)
    {
        SkillsPerLevel.put(Level, skills);
    }

    public void addSkillToLevel(int Level, MyPetSkillTreeSkill skill)
    {
        if (SkillsPerLevel.containsKey(Level))
        {
            SkillsPerLevel.get(Level).add(skill);
        }
        else
        {
            addLevel(Level, skill);
        }
    }

    public void addSkillToLevel(int Level, List<MyPetSkillTreeSkill> skills)
    {
        if (SkillsPerLevel.containsKey(Level))
        {
            SkillsPerLevel.get(Level).addAll(skills);
        }
        else
        {
            addLevel(Level, skills);
        }
    }

    public void addSkillToLevel(int Level, MyPetSkillTreeSkill[] skills)
    {
        if (SkillsPerLevel.containsKey(Level))
        {
            for (MyPetSkillTreeSkill skillname : skills)
            {
                SkillsPerLevel.get(Level).add(skillname);
            }
        }
        else
        {
            for (MyPetSkillTreeSkill skillname : skills)
            {
                addSkillToLevel(Level, skillname);
            }
        }
    }

    public Integer[] getAllLevel()
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

    public MyPetSkillTreeSkill[] getSkills(int Level)
    {
        if (SkillsPerLevel.containsKey(Level) && SkillsPerLevel.get(Level).size() > 0)
        {
            MyPetSkillTreeSkill[] SN = new MyPetSkillTreeSkill[SkillsPerLevel.get(Level).size()];
            for (int i = 0 ; i < SkillsPerLevel.get(Level).size() ; i++)
            {
                SN[i] = SkillsPerLevel.get(Level).get(i);
            }
            return SN;
        }
        return new MyPetSkillTreeSkill[0];
    }
}
/*
 * Copyright (C) 2011-2013 Keyle
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
    private String skillTreeName;
    protected String inheritance = null;

    private SortedMap<Integer, MyPetSkillTreeLevel> skillsPerLevel = new TreeMap<Integer, MyPetSkillTreeLevel>();

    public MyPetSkillTree(String name)
    {
        this.skillTreeName = name;
    }

    public MyPetSkillTree(String name, String inheritance)
    {
        this.skillTreeName = name;
        this.inheritance = inheritance;
    }

    public String getName()
    {
        return skillTreeName;
    }

    public boolean hasLevel(int level)
    {
        return skillsPerLevel.containsKey(level);
    }

    public MyPetSkillTreeLevel getLevel(int level)
    {
        if (!skillsPerLevel.containsKey(level))
        {
            return null;
        }
        return skillsPerLevel.get(level);
    }

    public MyPetSkillTreeLevel addLevel(int level)
    {
        if (!skillsPerLevel.containsKey(level))
        {
            MyPetSkillTreeLevel newLevel = new MyPetSkillTreeLevel(level);
            skillsPerLevel.put(level, newLevel);
            return newLevel;
        }
        return skillsPerLevel.get(level);
    }

    public void removeLevel(int level)
    {
        if (skillsPerLevel.containsKey(level))
        {
            skillsPerLevel.remove(level);
        }
    }

    public void addSkillToLevel(int level, MyPetSkillTreeSkill skill)
    {
        addLevel(level).addSkill(skill);
    }

    public void addSkillToLevel(int level, List<MyPetSkillTreeSkill> skillList)
    {
        MyPetSkillTreeLevel myPetSkillTreeLevel = addLevel(level);
        for (MyPetSkillTreeSkill skill : skillList)
        {
            myPetSkillTreeLevel.addSkill(skill);
        }
    }

    public List<MyPetSkillTreeLevel> getLevelList()
    {
        List<MyPetSkillTreeLevel> levelList = new ArrayList<MyPetSkillTreeLevel>();
        if (skillsPerLevel.size() > 0)
        {
            for (int level : skillsPerLevel.keySet())
            {
                levelList.add(skillsPerLevel.get(level));
            }
        }
        return levelList;
    }

    public String getInheritance()
    {
        return inheritance;
    }

    public boolean hasInheritance()
    {
        return inheritance != null;
    }
}
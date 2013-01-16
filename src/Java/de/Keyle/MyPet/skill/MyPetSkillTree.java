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
    private String permission = null;
    private String displayName = null;
    private short place = 0;
    private SortedMap<Short, MyPetSkillTreeLevel> skillsPerLevel = new TreeMap<Short, MyPetSkillTreeLevel>();

    public MyPetSkillTree(String name, short place)
    {
        this.skillTreeName = name;
        this.place = place;
    }

    public MyPetSkillTree(String name, String inheritance, short place)
    {
        this.skillTreeName = name;
        this.inheritance = inheritance;
        this.place = place;
    }

    public String getName()
    {
        return skillTreeName;
    }

    public short getPlace()
    {
        return place;
    }

    public boolean hasLevel(short level)
    {
        return skillsPerLevel.containsKey(level);
    }

    public MyPetSkillTreeLevel getLevel(short level)
    {
        if (!skillsPerLevel.containsKey(level))
        {
            return null;
        }
        return skillsPerLevel.get(level);
    }

    public MyPetSkillTreeLevel addLevel(short level)
    {
        if (!skillsPerLevel.containsKey(level))
        {
            MyPetSkillTreeLevel newLevel = new MyPetSkillTreeLevel(level);
            skillsPerLevel.put(level, newLevel);
            return newLevel;
        }
        return skillsPerLevel.get(level);
    }

    public MyPetSkillTreeLevel addLevel(MyPetSkillTreeLevel level)
    {
        if (!skillsPerLevel.containsKey(level.getLevel()))
        {
            skillsPerLevel.put(level.getLevel(), level);
            return level;
        }
        return skillsPerLevel.get(level);
    }

    public void removeLevel(short level)
    {
        if (skillsPerLevel.containsKey(level))
        {
            skillsPerLevel.remove(level);
        }
    }

    public void addSkillToLevel(short level, MyPetSkillTreeSkill skill)
    {
        addLevel(level).addSkill(skill);
    }

    public void addSkillToLevel(short level, List<MyPetSkillTreeSkill> skillList)
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
            for (short level : skillsPerLevel.keySet())
            {
                levelList.add(skillsPerLevel.get(level));
            }
        }
        return levelList;
    }

    public String getDisplayName()
    {
        if (displayName == null)
        {
            return skillTreeName;
        }
        return displayName;
    }

    public boolean hasDisplayName()
    {
        return displayName != null;
    }

    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    public String getPermission()
    {
        if (permission == null)
        {
            return skillTreeName;
        }
        return permission;
    }

    public boolean hasCustomPermissions()
    {
        return permission != null;
    }

    public void setPermission(String permission)
    {
        this.permission = permission;
    }

    public String getInheritance()
    {
        return inheritance;
    }

    public void setInheritance(String inheritance)
    {
        this.inheritance = inheritance;
    }

    public boolean hasInheritance()
    {
        return inheritance != null;
    }

    public MyPetSkillTree clone()
    {
        return clone(skillTreeName, place);
    }

    public MyPetSkillTree clone(String toName, short toPlace)
    {
        MyPetSkillTree newSkillTree = new MyPetSkillTree(toName, toPlace);
        newSkillTree.setInheritance(inheritance);
        newSkillTree.setDisplayName(displayName);
        newSkillTree.setPermission(permission);

        for (short level : skillsPerLevel.keySet())
        {
            newSkillTree.addLevel(skillsPerLevel.get(level).clone());
        }

        return newSkillTree;
    }
}
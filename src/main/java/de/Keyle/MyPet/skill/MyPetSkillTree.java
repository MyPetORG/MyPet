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

package de.Keyle.MyPet.skill;

import de.Keyle.MyPet.skill.skills.info.ISkillInfo;
import de.Keyle.MyPet.util.logger.MyPetLogger;

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

    public MyPetSkillTreeLevel addLevel(MyPetSkillTreeLevel level)
    {
        if (!skillsPerLevel.containsKey(level.getLevel()))
        {
            skillsPerLevel.put(level.getLevel(), level);
            return level;
        }
        return skillsPerLevel.get(level.getLevel());
    }

    public void removeLevel(int level)
    {
        if (skillsPerLevel.containsKey(level))
        {
            skillsPerLevel.remove(level);
        }
    }

    public void addSkillToLevel(int level, ISkillInfo skill)
    {
        if (skill == null)
        {
            MyPetLogger.write("Skills->null:level " + level);
        }
        addLevel(level).addSkill(skill);
    }

    public void addSkillToLevel(int level, List<ISkillInfo> skillList)
    {
        MyPetSkillTreeLevel myPetSkillTreeLevel = addLevel(level);
        for (ISkillInfo skill : skillList)
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
        return clone(skillTreeName);
    }

    public MyPetSkillTree clone(String toName)
    {
        MyPetSkillTree newSkillTree = new MyPetSkillTree(toName);
        newSkillTree.setInheritance(inheritance);
        newSkillTree.setDisplayName(displayName);
        newSkillTree.setPermission(permission);

        for (int level : skillsPerLevel.keySet())
        {
            newSkillTree.addLevel(skillsPerLevel.get(level).clone());
        }

        return newSkillTree;
    }
}
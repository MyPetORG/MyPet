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

import de.Keyle.MyPet.entity.types.MyPetType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyPetSkillTreeMobType
{
    private Map<String, MyPetSkillTree> skillTrees = new HashMap<String, MyPetSkillTree>();
    private List<String> skillTreeList = new ArrayList<String>();
    private String mobTypeName;

    private static Map<String, MyPetSkillTreeMobType> mobTypes = new HashMap<String, MyPetSkillTreeMobType>();

    public MyPetSkillTreeMobType(String mobTypeName)
    {
        this.mobTypeName = mobTypeName.toLowerCase();
        mobTypes.put(this.mobTypeName, this);
    }

    public String getMobTypeName()
    {
        return mobTypeName;
    }

    public void addSkillTree(String skillTreeName)
    {
        if (!skillTrees.containsKey(skillTreeName))
        {
            skillTrees.put(skillTreeName, new MyPetSkillTree(skillTreeName));
            skillTreeList.add(skillTreeName);
        }
    }

    public void addSkillTree(MyPetSkillTree skillTree)
    {
        if (!skillTrees.containsKey(skillTree.getName()))
        {
            skillTrees.put(skillTree.getName(), skillTree);
            skillTreeList.add(skillTree.getName());
        }
    }

    public void removeSkillTree(String skillTreeName)
    {
        if (skillTrees.containsKey(skillTreeName))
        {
            skillTrees.remove(skillTreeName);
        }
    }

    public void moveSkillTreeUp(String skillTreeName)
    {
        int index = skillTreeList.indexOf(skillTreeName);
        if (index != -1 && index > 0 && index < skillTreeList.size())
        {
            String skillTree = skillTreeList.get(index - 1);
            skillTreeList.set(index - 1, skillTreeName);
            skillTreeList.set(index, skillTree);

        }
    }

    public void moveSkillTreeDown(String skillTreeName)
    {
        int index = skillTreeList.indexOf(skillTreeName);
        if (index != -1 && index > 0 && index < skillTreeList.size())
        {
            String skillTree = skillTreeList.get(index + 1);
            skillTreeList.set(index + 1, skillTreeName);
            skillTreeList.set(index, skillTree);

        }
    }

    public MyPetSkillTree getSkillTree(String skillTreeName)
    {
        if (skillTrees.containsKey(skillTreeName))
        {
            return skillTrees.get(skillTreeName);
        }
        return null;
    }

    public boolean hasSkillTree(String skillTreeName)
    {
        return skillTrees.containsKey(skillTreeName);
    }

    public List<String> getSkillTreeNames()
    {
        return skillTreeList;
    }

    public static MyPetSkillTreeMobType getMobTypeByName(String mobTypeName)
    {
        return mobTypes.get(mobTypeName.toLowerCase());
    }

    public static MyPetSkillTreeMobType getMobTypeByPetType(MyPetType myPetType)
    {
        return mobTypes.get(myPetType.getTypeName().toLowerCase());
    }

    public static boolean hasMobType(String mobTypeName)
    {
        return mobTypes.containsKey(mobTypeName.toLowerCase());
    }

    public static List<String> getSkillTreeNames(MyPetType myPetType)
    {
        return getSkillTreeNames(myPetType.getTypeName().toLowerCase());
    }

    public static List<String> getSkillTreeNames(String myPetTypeName)
    {
        List<String> skillTreeNames;
        if (mobTypes.containsKey(myPetTypeName.toLowerCase()))
        {
            skillTreeNames = getMobTypeByName(myPetTypeName.toLowerCase()).getSkillTreeNames();
        }
        else
        {
            skillTreeNames = new ArrayList<String>();
        }
        return skillTreeNames;
    }

    public static boolean containsSkillTree(String myPetTypeName, String name)
    {
        return mobTypes.containsKey(myPetTypeName.toLowerCase()) && getMobTypeByName(myPetTypeName.toLowerCase()).getSkillTreeNames().indexOf(name) != -1;
    }
}

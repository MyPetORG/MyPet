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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyPetSkillTreeMobType
{
    private Map<String, MyPetSkillTree> skillTrees = new HashMap<String, MyPetSkillTree>();
    private String mobTypeName;

    private static Map<String, MyPetSkillTreeMobType> mobTypes = new HashMap<String, MyPetSkillTreeMobType>();

    public MyPetSkillTreeMobType(String mobTypeName)
    {
        this.mobTypeName = mobTypeName;
        mobTypes.put(mobTypeName, this);
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
        }
    }

    public void addSkillTree(MyPetSkillTree skillTree)
    {
        if (!skillTrees.containsKey(skillTree.getName()))
        {
            skillTrees.put(skillTree.getName(), skillTree);
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
        List<String> names = new ArrayList<String>();
        for (String skillTreeName : skillTrees.keySet())
        {
            names.add(skillTreeName);
        }
        return names;
    }

    public static MyPetSkillTreeMobType getMobTypeByName(String mobTypeName)
    {
        return mobTypes.get(mobTypeName);
    }
}

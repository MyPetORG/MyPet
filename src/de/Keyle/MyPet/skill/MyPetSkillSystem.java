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

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.util.MyPetUtil;

import java.util.*;

public class MyPetSkillSystem
{
    private static List<Class<? extends MyPetGenericSkill>> ClassSkillList = new ArrayList<Class<? extends MyPetGenericSkill>>();
    private static List<String> skillNames = new ArrayList<String>();

    private MyPet MPet;

    private Map<String, MyPetGenericSkill> Skills = new HashMap<String, MyPetGenericSkill>();

    public static void registerSkill(Class<? extends MyPetGenericSkill> clazz)
    {
        if (!ClassSkillList.contains(clazz))
        {
            try
            {
                Object obj = clazz.newInstance();
                if (obj instanceof MyPetGenericSkill)
                {
                    MyPetGenericSkill skill = (MyPetGenericSkill) obj;
                    skillNames.add(skill.getName().toLowerCase());
                    ClassSkillList.add(clazz);
                }
            }
            catch (Exception e)
            {
                MyPetUtil.getLogger().warning(clazz.getName() + "is not a valid skill!");
            }

        }
    }

    public static boolean isValidSkill(String name)
    {
        return skillNames.contains(name.toLowerCase());
    }

    public MyPetSkillSystem(MyPet MPet)
    {
        this.MPet = MPet;
        addSkills(ClassSkillList);
    }

    public void addSkill(Class<? extends MyPetGenericSkill> clazz)
    {
        String Name;
        MyPetGenericSkill skill;

        try
        {
            Object obj = clazz.newInstance();
            if (obj instanceof MyPetGenericSkill)
            {
                skill = (MyPetGenericSkill) obj;
                Name = skill.getName();

                if (!Skills.containsKey(Name))
                {
                    skill.MPet = this.MPet;
                    Skills.put(Name, skill);
                }
            }
        }
        catch (Exception e)
        {
            MyPetUtil.getLogger().warning(clazz.getName() + "is not a valid skill!");
            ClassSkillList.remove(clazz);
        }
    }

    public void addSkills(List<Class<? extends MyPetGenericSkill>> classList)
    {
        if (classList.size() > 0)
        {
            for (Class<? extends MyPetGenericSkill> cls : classList)
            {
                addSkill(cls);
            }
        }
    }

    public MyPetGenericSkill getSkill(String Name)
    {
        if (Skills.containsKey(Name))
        {
            return Skills.get(Name);
        }
        return null;
    }

    public Collection<MyPetGenericSkill> getSkills()
    {
        return Skills.values();
    }

    public Set<String> getSkillNames()
    {
        return Skills.keySet();
    }

    public boolean hasSkill(String Name)
    {
        return Skills.containsKey(Name);
    }
}
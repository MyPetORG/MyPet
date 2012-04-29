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

import de.Keyle.MyWolf.MyWolf;
import de.Keyle.MyWolf.util.MyWolfUtil;

import java.util.*;

public class MyWolfSkillSystem
{
    private static List<Class<? extends MyWolfGenericSkill>> ClassSkillList = new ArrayList<Class<? extends MyWolfGenericSkill>>();

    private MyWolf MWolf;

    private Map<String, MyWolfGenericSkill> Skills = new HashMap<String, MyWolfGenericSkill>();

    public static void registerSkill(Class<? extends MyWolfGenericSkill> cls)
    {
        if (!ClassSkillList.contains(cls))
        {
            ClassSkillList.add(cls);
        }
    }

    public MyWolfSkillSystem(MyWolf MWolf)
    {
        this.MWolf = MWolf;
        addSkills(ClassSkillList);
    }

    public void addSkill(Class<? extends MyWolfGenericSkill> clazz)
    {
        String Name;
        MyWolfGenericSkill Skill;

        try
        {
            Object obj = clazz.newInstance();
            if (obj instanceof MyWolfGenericSkill)
            {
                Skill = (MyWolfGenericSkill) obj;
                Name = Skill.getName();

                if (!Skills.containsKey(Name))
                {
                    Skill.MWolf = this.MWolf;
                    Skills.put(Name, Skill);
                }
            }
        }
        catch (Exception e)
        {
            MyWolfUtil.getLogger().warning(clazz.getName() + "is no valid skill!");
            ClassSkillList.remove(clazz);
        }
    }

    public void addSkills(List<Class<? extends MyWolfGenericSkill>> classList)
    {
        if (classList.size() > 0)
        {
            for (Class<? extends MyWolfGenericSkill> cls : classList)
            {
                addSkill(cls);
            }
        }
    }

    public MyWolfGenericSkill getSkill(String Name)
    {
        if (Skills.containsKey(Name))
        {
            return Skills.get(Name);
        }
        return null;
    }

    public Collection<MyWolfGenericSkill> getSkills()
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
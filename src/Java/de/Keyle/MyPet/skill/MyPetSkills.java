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
import de.Keyle.MyPet.skill.skills.MyPetGenericSkill;
import de.Keyle.MyPet.util.MyPetUtil;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import org.bukkit.ChatColor;

import java.util.*;

public class MyPetSkills
{
    private static List<Class<? extends MyPetGenericSkill>> skillClassList = new ArrayList<Class<? extends MyPetGenericSkill>>();
    private static List<String> skillNames = new ArrayList<String>();

    private MyPet myPet;

    private Map<String, MyPetGenericSkill> skills = new HashMap<String, MyPetGenericSkill>();

    public static void registerSkill(Class<? extends MyPetGenericSkill> clazz)
    {
        if (!skillClassList.contains(clazz))
        {
            try
            {
                Object obj = clazz.newInstance();
                if (obj instanceof MyPetGenericSkill)
                {
                    MyPetGenericSkill skill = (MyPetGenericSkill) obj;
                    skillNames.add(skill.getName().toLowerCase());
                    skillClassList.add(clazz);
                }
            }
            catch (Exception e)
            {
                MyPetLogger.write(ChatColor.RED + clazz.getName() + "is not a valid skill!");
                MyPetUtil.getDebugLogger().warning(clazz.getName() + "is not a valid skill!");
            }

        }
    }

    public static boolean isValidSkill(String name)
    {
        return skillNames.contains(name.toLowerCase());
    }

    public MyPetSkills(MyPet myPet)
    {
        this.myPet = myPet;
        addSkills(skillClassList);
    }

    public void addSkill(Class<? extends MyPetGenericSkill> skillClass)
    {
        String skillName;
        MyPetGenericSkill skill;

        try
        {
            Object obj = skillClass.newInstance();
            if (obj instanceof MyPetGenericSkill)
            {
                skill = (MyPetGenericSkill) obj;
                skillName = skill.getName();

                if (!skills.containsKey(skillName))
                {
                    skill.setMyPet(this.myPet);
                    skills.put(skillName, skill);
                }
            }
        }
        catch (Exception e)
        {
            MyPetLogger.write(ChatColor.RED + skillClass.getName() + "is not a valid skill!");
            MyPetUtil.getDebugLogger().warning(skillClass.getName() + "is not a valid skill!");
            skillClassList.remove(skillClass);
        }
    }

    public void addSkills(List<Class<? extends MyPetGenericSkill>> classList)
    {
        if (classList.size() > 0)
        {
            for (Class<? extends MyPetGenericSkill> clazz : classList)
            {
                addSkill(clazz);
            }
        }
    }

    public MyPetGenericSkill getSkill(String skillName)
    {
        if (skills.containsKey(skillName))
        {
            return skills.get(skillName);
        }
        return null;
    }

    public Collection<MyPetGenericSkill> getSkills()
    {
        return skills.values();
    }

    public Set<String> getSkillNames()
    {
        return skills.keySet();
    }

    public boolean hasSkill(String skillName)
    {
        return skills.containsKey(skillName);
    }

    public int getSkillLevel(String skillName)
    {
        if (hasSkill(skillName))
        {
            return getSkill(skillName).getLevel();
        }
        return -1;
    }

    public void reset()
    {
        for (MyPetGenericSkill skill : skills.values())
        {
            skill.reset();
        }
    }
}
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

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.util.MyPetUtil;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import org.bukkit.ChatColor;

import java.lang.reflect.Constructor;
import java.util.*;

public class MyPetSkills
{
    private static List<Class<? extends MyPetGenericSkill>> skillClassList = new ArrayList<Class<? extends MyPetGenericSkill>>();
    private static List<String> skillNames = new ArrayList<String>();
    private static Map<String, Class<? extends MyPetGenericSkill>> skillMap = new HashMap<String, Class<? extends MyPetGenericSkill>>();

    private MyPet myPet;

    private Map<String, MyPetGenericSkill> skills = new HashMap<String, MyPetGenericSkill>();

    public static void registerSkill(Class<? extends MyPetGenericSkill> clazz)
    {
        if (!skillClassList.contains(clazz))
        {
            try
            {
                Constructor<?> ctor = clazz.getConstructor(boolean.class);
                Object obj = ctor.newInstance(false);
                if (clazz.getAnnotation(SkillName.class) != null && obj instanceof MyPetGenericSkill)
                {
                    MyPetGenericSkill skill = (MyPetGenericSkill) obj;
                    skillNames.add(skill.getName().toLowerCase());
                    skillClassList.add(clazz);
                    skillMap.put(skill.getName().toLowerCase(), clazz);
                    if (MyPetUtil.getDebugLogger() != null)
                    {
                        MyPetUtil.getDebugLogger().info("registered skill: " + clazz.getName());
                    }
                }
            }
            catch (Exception e)
            {
                MyPetLogger.write(ChatColor.RED + clazz.getName() + " is not a valid skill!");
                if (MyPetUtil.getDebugLogger() != null)
                {
                    MyPetUtil.getDebugLogger().warning(clazz.getName() + " is not a valid skill!");
                }
            }

        }
    }

    public static boolean isValidSkill(String name)
    {
        return skillNames.contains(name.toLowerCase());
    }

    public static Class<? extends MyPetGenericSkill> getSkillClass(String name)
    {
        if (isValidSkill(name))
        {
            return skillMap.get(name.toLowerCase());
        }
        return null;
    }

    public static MyPetSkillTreeSkill getNewSkillInstance(String name)
    {
        return getNewSkillInstance(name, false);
    }

    public static MyPetSkillTreeSkill getNewSkillInstance(String name, boolean is)
    {
        try
        {
            Constructor<?> ctor = getSkillClass(name).getConstructor(boolean.class);
            Object obj = ctor.newInstance(is);
            if (obj instanceof MyPetSkillTreeSkill)
            {
                return (MyPetSkillTreeSkill) obj;
            }
        }
        catch (Exception e)
        {
            MyPetLogger.write(ChatColor.RED + getSkillClass(name).getName() + " is no valid Skill)!");
            MyPetUtil.getDebugLogger().warning(getSkillClass(name).getName() + " is no valid Skill!");
            e.printStackTrace();
        }
        return null;
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
            Constructor<?> ctor = skillClass.getConstructor(boolean.class);
            Object obj = ctor.newInstance(false);
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
            MyPetLogger.write(ChatColor.RED + skillClass.getName() + " is not a valid skill!");
            MyPetUtil.getDebugLogger().warning(skillClass.getName() + " is not a valid skill!");
            skillClassList.remove(skillClass);
        }
    }

    public void addSkills(List<Class<? extends MyPetGenericSkill>> classList)
    {
        for (Class<? extends MyPetGenericSkill> clazz : classList)
        {
            addSkill(clazz);
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

    public boolean isSkillActive(String skillName)
    {
        return hasSkill(skillName) && getSkill(skillName).isActive();
    }

    public void reset()
    {
        for (MyPetGenericSkill skill : skills.values())
        {
            skill.reset();
        }
    }
}
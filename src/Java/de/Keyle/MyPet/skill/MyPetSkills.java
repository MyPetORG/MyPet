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

import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.skill.skills.implementation.ISkillInstance;
import de.Keyle.MyPet.util.logger.DebugLogger;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import org.bukkit.ChatColor;

import java.lang.reflect.Constructor;
import java.util.*;

public class MyPetSkills
{
    private static List<Class<? extends MyPetSkillTreeSkill>> skillClassList = new ArrayList<Class<? extends MyPetSkillTreeSkill>>();
    private static List<String> skillNames = new ArrayList<String>();
    private static Map<String, Class<? extends MyPetSkillTreeSkill>> skillMap = new HashMap<String, Class<? extends MyPetSkillTreeSkill>>();

    private MyPet myPet;

    private Map<String, ISkillInstance> skills = new HashMap<String, ISkillInstance>();

    public static void registerSkill(Class<? extends MyPetSkillTreeSkill> clazz)
    {
        if (!skillClassList.contains(clazz))
        {
            try
            {
                Constructor<?> ctor = clazz.getConstructor(boolean.class);
                Object obj = ctor.newInstance(false);
                //MyPetLogger.write("Annotations2: " + Arrays.toString(clazz.getAnnotations()));
                if (clazz.getAnnotation(SkillName.class) != null && obj instanceof ISkillInstance)
                {
                    MyPetSkillTreeSkill skill = (MyPetSkillTreeSkill) obj;
                    skillNames.add(skill.getName().toLowerCase());
                    skillClassList.add(clazz);
                    skillMap.put(skill.getName().toLowerCase(), clazz);
                    DebugLogger.info("registered skill: " + clazz.getName());
                }
                else
                {
                    System.out.println("Don't worry - be happy");
                }
            }
            catch (Exception e)
            {
                MyPetLogger.write(ChatColor.RED + clazz.getName() + " is not a valid skill!");
                DebugLogger.warning(clazz.getName() + " is not a valid skill!");
            }

        }
    }

    @SuppressWarnings("unchecked")
    public static List<Class<? extends MyPetSkillTreeSkill>> getRegisteredSkills()
    {
        return skillClassList;
    }

    public static boolean isValidSkill(String name)
    {
        return skillNames.contains(name.toLowerCase());
    }

    public static Class<? extends MyPetSkillTreeSkill> getSkillClass(String name)
    {
        if (isValidSkill(name))
        {
            return skillMap.get(name.toLowerCase());
        }
        return null;
    }

    public static ISkillInstance getNewSkillInstance(String name)
    {
        return getNewSkillInstance(name, false);
    }

    public static ISkillInstance getNewSkillInstance(String name, boolean is)
    {
        try
        {
            Constructor<?> ctor = getSkillClass(name).getConstructor(boolean.class);
            Object obj = ctor.newInstance(is);
            if (obj instanceof MyPetSkillTreeSkill)
            {
                return (ISkillInstance) obj;
            }
        }
        catch (Exception e)
        {
            MyPetLogger.write(ChatColor.RED + getSkillClass(name).getName() + " is no valid Skill)!");
            DebugLogger.warning(getSkillClass(name).getName() + " is no valid Skill!");
            e.printStackTrace();
        }
        return null;
    }

    public MyPetSkills(MyPet myPet)
    {
        this.myPet = myPet;
        addSkills(skillClassList);
    }

    public void addSkill(Class<? extends MyPetSkillTreeSkill> skillClass)
    {
        String skillName;
        ISkillInstance skill;

        try
        {
            Constructor<?> ctor = skillClass.getConstructor(boolean.class);
            Object obj = ctor.newInstance(false);
            if (obj instanceof ISkillInstance)
            {
                skill = (ISkillInstance) obj;
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
            DebugLogger.warning(skillClass.getName() + " is not a valid skill!");
            skillClassList.remove(skillClass);
        }
    }

    public void addSkills(List<Class<? extends MyPetSkillTreeSkill>> classList)
    {
        for (Class<? extends MyPetSkillTreeSkill> clazz : classList)
        {
            addSkill(clazz);
        }
    }

    public ISkillInstance getSkill(String skillName)
    {
        if (skills.containsKey(skillName))
        {
            return skills.get(skillName);
        }
        return null;
    }

    public Collection<ISkillInstance> getSkills()
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
        for (ISkillInstance skill : skills.values())
        {
            skill.reset();
        }
    }
}
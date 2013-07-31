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

package de.Keyle.MyPet.skill.skills;

import de.Keyle.MyPet.skill.skills.info.ISkillInfo;
import de.Keyle.MyPet.skill.skilltree.SkillTreeSkill;
import de.Keyle.MyPet.util.logger.DebugLogger;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import org.bukkit.ChatColor;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkillsInfo
{
    private static List<Class<? extends SkillTreeSkill>> skillClassList = new ArrayList<Class<? extends SkillTreeSkill>>();
    private static List<String> skillNames = new ArrayList<String>();
    private static Map<String, Class<? extends SkillTreeSkill>> skillMap = new HashMap<String, Class<? extends SkillTreeSkill>>();

    public static void registerSkill(Class<? extends SkillTreeSkill> clazz)
    {
        if (!skillClassList.contains(clazz))
        {
            try
            {
                Constructor<?> ctor = clazz.getConstructor(boolean.class);
                Object obj = ctor.newInstance(false);
                if (clazz.getAnnotation(SkillName.class) != null && obj instanceof ISkillInfo)
                {
                    SkillTreeSkill skill = (SkillTreeSkill) obj;
                    skillNames.add(skill.getName());
                    skillClassList.add(clazz);
                    skillMap.put(skill.getName(), clazz);
                    //DebugLogger.info("registered info skill: " + clazz.getName());
                }
            }
            catch (Exception e)
            {
                MyPetLogger.write(ChatColor.RED + clazz.getName() + " is not a valid info skill!");
            }

        }
    }

    @SuppressWarnings("unchecked")
    public static List<Class<? extends SkillTreeSkill>> getRegisteredSkillsInfo()
    {
        return skillClassList;
    }

    public static boolean isValidSkill(String name)
    {
        return skillNames.contains(name);
    }

    public static Class<? extends SkillTreeSkill> getSkillInfoClass(String name)
    {
        if (isValidSkill(name))
        {
            return skillMap.get(name);
        }
        return null;
    }

    public static ISkillInfo getNewSkillInfoInstance(String name)
    {
        return getNewSkillInstance(name, false);
    }

    public static ISkillInfo getNewSkillInstance(String name, boolean is)
    {
        if (getSkillInfoClass(name) == null)
        {
            DebugLogger.warning(name + " skill is null when creating new instance!");
            for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace())
            {
                DebugLogger.warning("  " + stackTraceElement.toString());
            }
            DebugLogger.warning("------------------");
            return null;
        }
        try
        {
            Constructor<?> ctor = getSkillInfoClass(name).getConstructor(boolean.class);
            Object obj = ctor.newInstance(is);
            if (obj instanceof ISkillInfo)
            {
                return (ISkillInfo) obj;
            }
        }
        catch (Exception e)
        {
            MyPetLogger.write(ChatColor.RED + getSkillInfoClass(name).getName() + " is no valid Skill)!");
            e.printStackTrace();
        }
        return null;
    }
}
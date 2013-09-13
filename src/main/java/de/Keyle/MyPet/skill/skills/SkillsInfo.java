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
import de.Keyle.MyPet.util.logger.MyPetLogger;
import org.bukkit.ChatColor;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SkillsInfo {
    private static Map<Class<? extends SkillTreeSkill>, String> registeredSkillsNames = new HashMap<Class<? extends SkillTreeSkill>, String>();
    private static Map<String, Class<? extends SkillTreeSkill>> registeredNamesSkills = new HashMap<String, Class<? extends SkillTreeSkill>>();

    public static void registerSkill(Class<? extends SkillTreeSkill> clazz) {
        if (!ISkillInfo.class.isAssignableFrom(clazz)) {
            MyPetLogger.write(ChatColor.RED + clazz.getName() + " doesn't implements [ISkillInfo]!");
            return;
        }
        try {
            //MyPetLogger.write("Skill Annotations: " + Arrays.toString(clazz.getAnnotations()));
            SkillName sn = clazz.getAnnotation(SkillName.class);
            if (sn != null) {
                String skillName = sn.value();
                if (!registeredNamesSkills.containsKey(skillName) && !registeredSkillsNames.containsKey(clazz)) {
                    registeredSkillsNames.put(clazz, skillName);
                    registeredNamesSkills.put(skillName, clazz);
                    //DebugLogger.info("registered skill: " + clazz.getName());
                } else {
                    MyPetLogger.write(ChatColor.RED + "There is already a skill registered with the the name " + skillName);
                }
            } else {
                MyPetLogger.write(ChatColor.RED + clazz.getName() + " is not annotated with [SkillName]!");
            }
        } catch (Exception e) {
            MyPetLogger.write(ChatColor.RED + clazz.getName() + " is not a valid skill!");
        }
    }

    @SuppressWarnings("unchecked")
    public static Set<Class<? extends SkillTreeSkill>> getRegisteredSkillsInfo() {
        return registeredSkillsNames.keySet();
    }

    public static boolean isValidSkill(Class<? extends SkillTreeSkill> clazz) {
        return ISkillInfo.class.isAssignableFrom(clazz) && clazz.getAnnotation(SkillName.class) != null;
    }

    public static Class<? extends SkillTreeSkill> getSkillInfoClass(String name) {
        return registeredNamesSkills.get(name);
    }

    public static ISkillInfo getNewSkillInfoInstance(String name) {
        Class<? extends SkillTreeSkill> clazz = getSkillInfoClass(name);
        if (clazz == null) {
            return null;
        }
        return getNewSkillInstance(clazz);
    }

    public static ISkillInfo getNewSkillInstance(Class<? extends SkillTreeSkill> clazz) {
        return getNewSkillInstance(clazz, false);
    }

    public static ISkillInfo getNewSkillInstance(Class<? extends SkillTreeSkill> clazz, boolean is) {
        if (clazz == null) {
            return null;
        }
        try {
            Constructor<?> ctor = clazz.getConstructor(boolean.class);
            Object obj = ctor.newInstance(is);
            if (obj != null) {
                return (ISkillInfo) obj;
            }
        } catch (Exception e) {
            MyPetLogger.write(ChatColor.RED + clazz.getName() + " is no valid Skill)!");
            e.printStackTrace();
        }
        return null;
    }
}
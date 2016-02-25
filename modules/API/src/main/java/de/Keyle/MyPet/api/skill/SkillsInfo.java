/*
 * This file is part of mypet-api
 *
 * Copyright (C) 2011-2016 Keyle
 * mypet-api is licensed under the GNU Lesser General Public License.
 *
 * mypet-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mypet-api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.api.skill;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.skill.skilltree.SkillTreeSkill;
import org.bukkit.ChatColor;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SkillsInfo {
    private static Map<Class<? extends SkillTreeSkill>, String> registeredSkillsNames = new HashMap<>();
    private static Map<String, Class<? extends SkillTreeSkill>> registeredNamesSkills = new HashMap<>();

    public static void registerSkill(Class<? extends SkillTreeSkill> clazz) {
        if (!SkillInfo.class.isAssignableFrom(clazz)) {
            MyPetApi.getLogger().warning(ChatColor.RED + clazz.getName() + " doesn't implements SkillInfo!");
            return;
        }
        try {
            //MyPetApi.getLogger().warning("Skill Annotations: " + Arrays.toString(clazz.getAnnotations()));
            SkillName sn = clazz.getAnnotation(SkillName.class);
            if (sn != null) {
                String skillName = sn.value();
                if (!registeredNamesSkills.containsKey(skillName) && !registeredSkillsNames.containsKey(clazz)) {
                    registeredSkillsNames.put(clazz, skillName);
                    registeredNamesSkills.put(skillName, clazz);
                    //DebugLogger.info("registered skill: " + clazz.getName());
                } else {
                    MyPetApi.getLogger().warning(ChatColor.RED + "There is already a skill registered with the the name " + skillName);
                }
            } else {
                MyPetApi.getLogger().warning(ChatColor.RED + clazz.getName() + " is not annotated with @SkillName!");
            }
        } catch (Exception e) {
            MyPetApi.getLogger().warning(ChatColor.RED + clazz.getName() + " is not a valid skill!");
        }
    }

    @SuppressWarnings("unchecked")
    public static Set<Class<? extends SkillTreeSkill>> getRegisteredSkillsInfo() {
        return registeredSkillsNames.keySet();
    }

    public static boolean isValidSkill(Class<? extends SkillTreeSkill> clazz) {
        return SkillInfo.class.isAssignableFrom(clazz) && clazz.getAnnotation(SkillName.class) != null;
    }

    public static Class<? extends SkillTreeSkill> getSkillInfoClass(String name) {
        return registeredNamesSkills.get(name);
    }

    public static SkillInfo getNewSkillInfoInstance(String name) {
        Class<? extends SkillTreeSkill> clazz = getSkillInfoClass(name);
        if (clazz == null) {
            return null;
        }
        return getNewSkillInstance(clazz);
    }

    public static SkillInfo getNewSkillInstance(Class<? extends SkillTreeSkill> clazz) {
        return getNewSkillInstance(clazz, false);
    }

    public static SkillInfo getNewSkillInstance(Class<? extends SkillTreeSkill> clazz, boolean is) {
        if (clazz == null) {
            return null;
        }
        try {
            Constructor<?> ctor = clazz.getConstructor(boolean.class);
            Object obj = ctor.newInstance(is);
            return (SkillInfo) obj;
        } catch (Exception e) {
            MyPetApi.getLogger().warning(ChatColor.RED + clazz.getName() + " is not a valid skill)!");
            e.printStackTrace();
        }
        return null;
    }
}
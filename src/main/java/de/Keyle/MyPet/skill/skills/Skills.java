/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.Keyle.MyPet.entity.types.MyPet;
import de.Keyle.MyPet.skill.skills.implementation.ISkillInstance;
import de.Keyle.MyPet.skill.skilltree.SkillTreeSkill;
import de.Keyle.MyPet.util.logger.DebugLogger;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import org.bukkit.ChatColor;

import java.lang.reflect.Constructor;
import java.util.Set;

public class Skills {
    private static BiMap<Class<? extends SkillTreeSkill>, String> registeredSkillsNames = HashBiMap.create();
    private static BiMap<String, Class<? extends SkillTreeSkill>> registeredNamesSkills = registeredSkillsNames.inverse();

    private MyPet myPet;

    private BiMap<String, ISkillInstance> skillsNamesClass = HashBiMap.create();
    private BiMap<ISkillInstance, String> skillsClassNames = skillsNamesClass.inverse();

    public static void registerSkill(Class<? extends SkillTreeSkill> clazz) {
        if (!ISkillInstance.class.isAssignableFrom(clazz)) {
            MyPetLogger.write(ChatColor.RED + clazz.getName() + " doesn't implements [ISkillInstance]!");
            return;
        }
        if (!registeredSkillsNames.containsKey(clazz)) {
            try {
                //MyPetLogger.write("Skill Annotations: " + Arrays.toString(clazz.getAnnotations()));
                SkillName sn = clazz.getAnnotation(SkillName.class);
                if (sn != null) {
                    String skillName = sn.value();
                    if (!registeredNamesSkills.containsKey(skillName)) {
                        registeredSkillsNames.put(clazz, skillName);
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
    }

    @SuppressWarnings("unchecked")
    public static Set<Class<? extends SkillTreeSkill>> getRegisteredSkills() {
        return registeredSkillsNames.keySet();
    }

    public static boolean isValidSkill(Class<? extends SkillTreeSkill> clazz) {
        return ISkillInstance.class.isAssignableFrom(clazz) && clazz.getAnnotation(SkillName.class) != null;
    }

    public static Class<? extends SkillTreeSkill> getSkillClass(String name) {
        return registeredNamesSkills.get(name);
    }

    /*
    public static ISkillInstance getNewSkillInstance(String name)
    {
        return getNewSkillInstance(getSkillClass(name), false);
    }
    */

    public static ISkillInstance getNewSkillInstance(Class<? extends SkillTreeSkill> clazz) {
        return getNewSkillInstance(clazz, false);
    }

    public static ISkillInstance getNewSkillInstance(Class<? extends SkillTreeSkill> clazz, boolean is) {
        if (clazz == null) {
            return null;
        }
        try {
            Constructor<?> ctor = clazz.getConstructor(boolean.class);
            Object obj = ctor.newInstance(is);
            return (ISkillInstance) obj;
        } catch (Exception e) {
            MyPetLogger.write(ChatColor.RED + clazz.getName() + " is no valid Skill)!");
            e.printStackTrace();
            DebugLogger.printThrowable(e);
        }
        return null;
    }

    public Skills(MyPet myPet) {
        this.myPet = myPet;
        try {
            for (Class<? extends SkillTreeSkill> clazz : registeredSkillsNames.keySet()) {
                addSkill(clazz);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addSkill(Class<? extends SkillTreeSkill> skillClass) {
        if (!isValidSkill(skillClass)) {
            MyPetLogger.write(ChatColor.RED + skillClass.getName() + " is not a valid skill!");
        }
        try {
            Constructor<?> ctor = skillClass.getConstructor(boolean.class);
            Object obj = ctor.newInstance(false);
            if (obj instanceof ISkillInstance) {
                ISkillInstance skill = (ISkillInstance) obj;
                String skillName = skill.getName();

                skill.setMyPet(this.myPet);
                skillsNamesClass.put(skillName, skill);
            }
        } catch (Exception e) {
            MyPetLogger.write(ChatColor.RED + skillClass.getName() + " is not a valid skill!");
            DebugLogger.printThrowable(e);
            registeredSkillsNames.remove(skillClass);
        }
    }

    public ISkillInstance getSkill(String skillName) {
        return skillsNamesClass.get(skillName);
    }

    @SuppressWarnings("unchecked")
    public <T extends ISkillInstance> T getSkill(Class<T> clazz) {
        SkillName sn = clazz.getAnnotation(SkillName.class);
        if (sn == null) {
            return null;
        }
        if (!skillsNamesClass.containsKey(sn.value())) {
            return null;
        }
        ISkillInstance skill = skillsNamesClass.get(sn.value());
        if (!clazz.isInstance(skill)) {
            return null;
        }
        return (T) skill;
    }

    public Set<ISkillInstance> getSkills() {
        return skillsClassNames.keySet();
    }

    public Set<String> getSkillNames() {
        return skillsNamesClass.keySet();
    }

    /*
    public boolean hasSkill(String skillName)
    {
        return skillsNamesClass.containsKey(skillName);
    }
    */

    public boolean hasSkill(Class<? extends ISkillInstance> clazz) {
        SkillName sn = clazz.getAnnotation(SkillName.class);
        if (sn == null) {
            return false;
        }
        if (!skillsNamesClass.containsKey(sn.value())) {
            return false;
        }
        return clazz.isInstance(skillsNamesClass.get(sn.value()));
    }

    /*
    public boolean isSkillActive(String skillName)
    {
        return hasSkill(skillName) && getSkill(skillName).isActive();
    }
    */

    public boolean isSkillActive(Class<? extends ISkillInstance> clazz) {
        return hasSkill(clazz) && getSkill(clazz).isActive();
    }

    public void reset() {
        for (ISkillInstance skill : skillsClassNames.keySet()) {
            skill.reset();
        }
    }
}
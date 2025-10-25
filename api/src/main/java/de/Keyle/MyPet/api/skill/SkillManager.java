/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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

package de.Keyle.MyPet.api.skill;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.skill.skilltree.Skill;
import de.Keyle.MyPet.api.util.service.Load;
import de.Keyle.MyPet.api.util.service.ServiceContainer;
import de.Keyle.MyPet.api.util.service.ServiceName;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@ServiceName("SkillManager")
@Load(Load.State.OnEnable)
public class SkillManager implements ServiceContainer {
    private Map<Class<? extends Skill>, String> registeredSkillsNames = new HashMap<>();
    private Map<String, Class<? extends Skill>> registeredNamesSkills = new HashMap<>();

    @Override
    public void onDisable() {
        registeredSkillsNames.clear();
    }

    public void registerSkill(Class<? extends Skill> clazz) {
        if (!Skill.class.isAssignableFrom(clazz)) {
            MyPetApi.getLogger().warning(clazz.getName() + " doesn't implements Skill!");
            return;
        }
        try {
            String skillName = getSkillName(clazz);
            if (skillName != null) {
                if (!registeredNamesSkills.containsKey(skillName) && !registeredSkillsNames.containsKey(clazz)) {
                    registeredSkillsNames.put(clazz, skillName);
                    registeredNamesSkills.put(skillName, clazz);
                } else {
                    MyPetApi.getLogger().warning("There is already a skill registered with the the name " + skillName);
                }
                return;
            }
            MyPetApi.getLogger().warning(clazz.getName() + " is not annotated with @SkillName!");
        } catch (Exception e) {
            MyPetApi.getLogger().warning(clazz.getName() + " is not a valid skill!");
        }
    }

    public Set<Class<? extends Skill>> getRegisteredSkills() {
        return registeredSkillsNames.keySet();
    }

    public boolean isValidSkill(Class clazz) {
        if (clazz == Object.class) {
            return false;
        }
        if (Skill.class.isAssignableFrom(clazz) && clazz.getAnnotation(SkillName.class) != null) {
            return true;
        }
        if (isValidSkill(clazz.getSuperclass())) {
            return true;
        }
        for (Class c : clazz.getInterfaces()) {
            if (isValidSkill(c)) {
                return true;
            }
        }
        return false;
    }

    public String getSkillName(Class clazz) {
        if (clazz == Object.class) {
            return null;
        }
        if (Skill.class.isAssignableFrom(clazz)) {
            SkillName sn = (SkillName) clazz.getAnnotation(SkillName.class);
            if (sn != null) {
                return sn.value();
            }
        }
        String skillName = getSkillName(clazz.getSuperclass());
        if (skillName != null) {
            return skillName;
        }
        for (Class c : clazz.getInterfaces()) {
            skillName = getSkillName(c);
            if (skillName != null) {
                return skillName;
            }
        }
        return null;
    }

    public Class<? extends Skill> getSkillClass(String name) {
        return registeredNamesSkills.get(name);
    }

    public Skill getNewSkillInstance(Class<? extends Skill> clazz, MyPet myPet) {
        if (clazz == null) {
            return null;
        }
        try {
            Constructor<?> ctor = clazz.getConstructor(MyPet.class);
            Object obj = ctor.newInstance(myPet);
            return (Skill) obj;
        } catch (Exception e) {
            MyPetApi.getLogger().warning(clazz.getName() + " is not a valid skill)!");
            e.printStackTrace();
        }
        return null;
    }
}
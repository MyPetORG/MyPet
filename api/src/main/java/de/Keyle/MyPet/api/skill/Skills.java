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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.skill.skilltree.Skill;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Skills {
    private BiMap<String, Skill> skills = HashBiMap.create();
    private Map<Class<? extends Skill>, Skill> skillClasses = new HashMap<>();

    public Skills(MyPet myPet) {
        for (Class<? extends Skill> clazz : MyPetApi.getSkillManager().getRegisteredSkills()) {
            try {
                Skill skill = MyPetApi.getSkillManager().getNewSkillInstance(clazz, myPet);
                skills.put(skill.getName(), skill);

                Set<Class<? extends Skill>> result = new HashSet<>();
                Util.getClassParents(clazz, Skill.class, result);

                for (Class<? extends Skill> c : result) {
                    skillClasses.put(c, skill);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Skill get(String skillName) {
        return skills.get(skillName);
    }

    @SuppressWarnings("unchecked")
    public <T extends Skill> T get(Class<T> clazz) {
        return (T) skillClasses.get(clazz);
    }

    public Set<Skill> all() {
        return skills.values();
    }

    public Set<String> getNames() {
        return skills.keySet();
    }

    public boolean has(String skillName) {
        return skills.containsKey(skillName);
    }

    public boolean has(Class<? extends Skill> clazz) {
        return skillClasses.containsKey(clazz);
    }

    public boolean isActive(String skillName) {
        Skill skill = get(skillName);
        return skill != null && skill.isActive();
    }

    public boolean isActive(Class<? extends Skill> clazz) {
        Skill skill = get(clazz);
        return skill != null && skill.isActive();
    }
}
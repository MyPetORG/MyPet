/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
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
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.skill.skilltree.Skill;
import de.Keyle.MyPet.api.util.ErrorUtil;
import de.Keyle.MyPet.api.util.Scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Container holding all {@link Skill} instances for a single {@link Pet}. Created
 * once per pet (typically when the pet is loaded or spawned), this class instantiates
 * every skill registered with the {@link SkillManager} and indexes them by both
 * name and class hierarchy for fast lookup.
 *
 * <p>Callers retrieve skills via {@link #get(String)} (by {@code @SkillName} value)
 * or {@link #get(Class)} (by skill class or parent interface), and can check
 * activation state with {@link #isActive(String)} / {@link #isActive(Class)}.
 *
 * @see SkillManager
 * @see Skill
 */
public class Skills {
    private final BiMap<String, Skill> skills = HashBiMap.create();
    private final Map<Class<? extends Skill>, Skill> skillClasses = new HashMap<>();
    private final List<OnHitSkill> onHitSkills;
    private final List<OnDamageByEntitySkill> onDamageByEntitySkills;
    private final List<Scheduler> schedulerSkills;

    /**
     * Creates a new skill container for the given pet, instantiating all
     * registered skills and indexing them by name and class hierarchy.
     *
     * @param pet the pet that owns these skill instances
     */
    public Skills(Pet pet) {
        List<OnHitSkill> onHit = new ArrayList<>();
        List<OnDamageByEntitySkill> onDamageByEntity = new ArrayList<>();
        List<Scheduler> schedulers = new ArrayList<>();
        for (Class<? extends Skill> clazz : MyPetApi.getSkillManager().getRegisteredSkills()) {
            try {
                Skill skill = MyPetApi.getSkillManager().getNewSkillInstance(clazz, pet);
                skills.put(skill.getName(), skill);

                Set<Class<? extends Skill>> result = new HashSet<>();
                Util.getClassParents(clazz, Skill.class, result);

                for (Class<? extends Skill> c : result) {
                    skillClasses.put(c, skill);
                }

                if (skill instanceof OnHitSkill onHitSkill) onHit.add(onHitSkill);
                if (skill instanceof OnDamageByEntitySkill onDamageSkill) onDamageByEntity.add(onDamageSkill);
                if (skill instanceof Scheduler scheduler) schedulers.add(scheduler);
            } catch (Exception e) {
                ErrorUtil.report(e);
            }
        }
        this.onHitSkills = List.copyOf(onHit);
        this.onDamageByEntitySkills = List.copyOf(onDamageByEntity);
        this.schedulerSkills = List.copyOf(schedulers);
    }

    /**
     * Looks up a skill by its {@link SkillName} value.
     *
     * @return the skill instance, or {@code null} if no skill with that name exists
     */
    public Skill get(String skillName) {
        return skills.get(skillName);
    }

    /**
     * Looks up a skill by class or parent interface. This allows retrieving a skill
     * via any type in its class hierarchy (e.g. requesting the abstract base class
     * or a marker interface).
     *
     * @param clazz the skill class or interface to look up
     * @return the matching skill instance, or {@code null} if not found
     */
    @SuppressWarnings("unchecked")
    public <T extends Skill> T get(Class<T> clazz) {
        return (T) skillClasses.get(clazz);
    }

    /** Returns all skill instances in this container. */
    public Set<Skill> all() {
        return skills.values();
    }

    /** Immutable list of all skills that implement {@link OnHitSkill}. */
    public List<OnHitSkill> getOnHitSkills() {
        return onHitSkills;
    }

    /** Immutable list of all skills that implement {@link OnDamageByEntitySkill}. */
    public List<OnDamageByEntitySkill> getOnDamageByEntitySkills() {
        return onDamageByEntitySkills;
    }

    /** Immutable list of all skills that implement {@link Scheduler}. */
    public List<Scheduler> getSchedulerSkills() {
        return schedulerSkills;
    }

    /** Returns the set of all skill names (from {@code @SkillName} annotations). */
    public Set<String> getNames() {
        return skills.keySet();
    }

    /** Returns {@code true} if a skill with the given name is registered. */
    public boolean has(String skillName) {
        return skills.containsKey(skillName);
    }

    /** Returns {@code true} if a skill matching the given class is registered. */
    public boolean has(Class<? extends Skill> clazz) {
        return skillClasses.containsKey(clazz);
    }

    /**
     * Returns {@code true} if the named skill exists and is currently active
     * (i.e. has been upgraded at least once and its conditions are met).
     */
    public boolean isActive(String skillName) {
        Skill skill = get(skillName);
        return skill != null && skill.isActive();
    }

    /**
     * Returns {@code true} if the skill matching the given class exists and is
     * currently active.
     */
    public boolean isActive(Class<? extends Skill> clazz) {
        Skill skill = get(clazz);
        return skill != null && skill.isActive();
    }
}
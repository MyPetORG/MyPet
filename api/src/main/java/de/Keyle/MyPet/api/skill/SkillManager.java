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
import de.Keyle.MyPet.api.util.ErrorUtil;
import de.Keyle.MyPet.api.util.service.Load;
import de.Keyle.MyPet.api.util.service.ServiceContainer;
import de.Keyle.MyPet.api.util.service.ServiceName;
import net.kyori.adventure.nbt.CompoundBinaryTag;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Central registry for all {@link Skill} classes, their {@link UpgradeParser}s, and
 * {@link SkillStateParser}s. Registered as a {@link ServiceContainer} that loads at
 * {@link Load.State#OnEnable}.
 *
 * <p>Skills are registered by class via {@link #registerSkill(Class)} during plugin
 * startup. Each skill class must carry (directly or via inheritance) a
 * {@link SkillName} annotation that provides the canonical name used as the JSON key
 * in {@code .st.json} skilltree files and in the {@link Skills} name-based lookup.
 *
 * <p>Upgrade parsers (registered via {@link #registerUpgradeParser}) convert skilltree
 * JSON nodes into {@link Upgrade} objects; state parsers (registered via
 * {@link #registerStateParser}) convert persisted NBT into typed {@link SkillState}
 * records.
 *
 * @see Skills
 * @see SkillName
 */
@ServiceName("SkillManager")
@Load(Load.State.OnEnable)
public class SkillManager implements ServiceContainer {
    private final Map<Class<? extends Skill>, String> registeredSkillsNames = new HashMap<>();
    private final Map<String, Class<? extends Skill>> registeredNamesSkills = new HashMap<>();
    private final Map<String, UpgradeParser<?>> upgradeParsers = new HashMap<>();
    private final Map<Class<? extends Skill>, SkillStateBinding<?>> stateParsers = new HashMap<>();

    /** Pairs the registered state class with its parser so {@link #parseState} can do a typed lookup keyed only on the skill class. */
    private record SkillStateBinding<T extends SkillState>(Class<T> stateClass, SkillStateParser<T> parser) {}

    @Override
    public void onDisable() {
        registeredSkillsNames.clear();
        registeredNamesSkills.clear();
        upgradeParsers.clear();
        stateParsers.clear();
    }

    /**
     * Registers a skill class with the manager. The class must implement {@link Skill}
     * and carry (directly or inherited) a {@link SkillName} annotation. Duplicate
     * registrations (by name or class) are logged and ignored.
     *
     * @param clazz the skill implementation class to register
     */
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

    /** Returns the set of all registered skill implementation classes. */
    public Set<Class<? extends Skill>> getRegisteredSkills() {
        return registeredSkillsNames.keySet();
    }

    /**
     * Recursively checks whether the given class is (or extends/implements) a valid
     * skill — i.e. it is assignable to {@link Skill} and carries a {@link SkillName}
     * annotation somewhere in its type hierarchy.
     *
     * @param clazz the class to inspect
     * @return {@code true} if it qualifies as a valid skill class
     */
    public boolean isValidSkill(Class<?> clazz) {
        if (clazz == Object.class) {
            return false;
        }
        if (Skill.class.isAssignableFrom(clazz) && clazz.getAnnotation(SkillName.class) != null) {
            return true;
        }
        if (isValidSkill(clazz.getSuperclass())) {
            return true;
        }
        for (Class<?> c : clazz.getInterfaces()) {
            if (isValidSkill(c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves the {@link SkillName#value()} for the given class by walking its
     * superclass chain and interfaces. Returns {@code null} if no annotation is found.
     *
     * @param clazz the class to inspect
     * @return the skill name, or {@code null} if not annotated
     */
    public String getSkillName(Class<?> clazz) {
        if (clazz == Object.class) {
            return null;
        }
        if (Skill.class.isAssignableFrom(clazz)) {
            SkillName sn = clazz.getAnnotation(SkillName.class);
            if (sn != null) {
                return sn.value();
            }
        }
        String skillName = getSkillName(clazz.getSuperclass());
        if (skillName != null) {
            return skillName;
        }
        for (Class<?> c : clazz.getInterfaces()) {
            skillName = getSkillName(c);
            if (skillName != null) {
                return skillName;
            }
        }
        return null;
    }

    /**
     * Returns the skill implementation class registered under the given name.
     *
     * @param name the canonical skill name (from {@code @SkillName})
     * @return the class, or {@code null} if no skill is registered with that name
     */
    public Class<? extends Skill> getSkillClass(String name) {
        return registeredNamesSkills.get(name);
    }

    /**
     * Creates a new instance of the given skill class for the specified pet. The skill
     * class must have a public constructor accepting a single {@link MyPet} parameter.
     *
     * @param clazz the skill implementation class
     * @param myPet the pet that will own the skill instance
     * @return the new skill instance, or {@code null} if instantiation fails
     */
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
            ErrorUtil.report(e);
        }
        return null;
    }

    /**
     * Registers a parser that builds an {@link Upgrade} from the JSON object
     * under {@code Skills.<name>.Upgrades.<levelRule>} in an {@code .st.json}
     * skilltree file.
     *
     * <p>The skill name used as the JSON-lookup key is read from the
     * {@code @SkillName} annotation on {@code skillClass} (walking superclasses
     * and interfaces if necessary). Re-registering the same skill silently
     * overwrites the previous parser; addons should call this exactly once
     * per skill, after registering the corresponding {@code Skill} class via
     * {@link #registerSkill}.
     *
     * @param skillClass any {@link Skill} type bearing a {@code @SkillName} —
     *                   typically the same class passed to {@link #registerSkill}
     * @param parser     the parser to register; must produce upgrades for
     *                   {@code S}
     * @throws IllegalArgumentException if {@code skillClass} has no
     *         {@code @SkillName} annotation in its type hierarchy
     */
    public <S extends Skill> void registerUpgradeParser(Class<S> skillClass, UpgradeParser<S> parser) {
        String skillName = getSkillName(skillClass);
        if (skillName == null) {
            throw new IllegalArgumentException(skillClass.getName() + " is not annotated with @SkillName");
        }
        upgradeParsers.put(skillName.toLowerCase(Locale.ROOT), parser);
    }

    /**
     * Returns the parser registered for the given skill name, or {@code null}
     * if none is registered. Lookup is case-insensitive. Used by the skilltree
     * loader to resolve a JSON {@code Skills.<name>} key to its parser.
     */
    public UpgradeParser<?> getUpgradeParser(String skillName) {
        return upgradeParsers.get(skillName.toLowerCase(Locale.ROOT));
    }

    /**
     * Registers the typed {@link SkillState} parser for {@code skillClass}.
     * Each skill may register at most one parser; the parser receives the
     * per-skill NBT compound (the value stored under the skill's name in the
     * aggregate {@code skillInfo}, not the aggregate itself) and returns the
     * skill's typed state record.
     *
     * <p>Replaces the pre-4.0.0 raw-NBT escape hatch.
     * Addons that store custom state on a {@link Skill} subclass register
     * here once at plugin enable.
     *
     * @throws IllegalArgumentException if a parser is already registered for
     *         {@code skillClass} (re-registration is a programming error,
     *         not a hot-reload feature)
     */
    public <S extends Skill, T extends SkillState> void registerStateParser(
            Class<S> skillClass, Class<T> stateClass, SkillStateParser<T> parser) {
        if (stateParsers.containsKey(skillClass)) {
            throw new IllegalArgumentException("A SkillStateParser is already registered for " + skillClass.getName());
        }
        stateParsers.put(skillClass, new SkillStateBinding<>(stateClass, parser));
    }

    /**
     * Parses {@code compound} into the typed {@link SkillState} for
     * {@code skillClass}, or returns {@link Optional#empty()} if no parser
     * is registered, the registered state class doesn't match
     * {@code stateClass}, or the parser declines the compound.
     *
     * <p>Called from {@code StoredMyPet#skillState} on the persisted-pet
     * branch; addons should not call this directly.
     */
    @SuppressWarnings("unchecked")
    public <S extends Skill, T extends SkillState> Optional<T> parseState(
            Class<S> skillClass, Class<T> stateClass, CompoundBinaryTag compound) {
        SkillStateBinding<?> binding = stateParsers.get(skillClass);
        if (binding == null || !stateClass.equals(binding.stateClass())) {
            return Optional.empty();
        }
        return ((SkillStateBinding<T>) binding).parser().parse(compound);
    }
}
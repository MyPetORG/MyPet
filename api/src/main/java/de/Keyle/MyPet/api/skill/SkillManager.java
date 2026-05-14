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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.skill.skilltree.Skill;
import de.Keyle.MyPet.api.util.AnnotationLookup;
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
 * {@link SkillStateCodec}s. Registered as a {@link ServiceContainer} that loads at
 * {@link Load.State#OnEnable}.
 *
 * <p>Skills are registered by class via {@link #registerSkill(Class)} during plugin
 * startup. Each skill class must carry (directly or via inheritance) a
 * {@link SkillName} annotation that provides the canonical name used as the JSON key
 * in {@code .st.json} skilltree files and in the {@link Skills} name-based lookup.
 *
 * <p>Upgrade parsers (registered via {@link #registerUpgradeParser}) convert skilltree
 * JSON nodes into {@link Upgrade} objects; state codecs (registered via
 * {@link #registerCodec}) round-trip persisted NBT to typed {@link SkillState}
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
    private final Map<Class<? extends Skill>, SkillStateCodecBinding<?>> stateCodecs = new HashMap<>();

    /** Pairs the registered state class with its codec so save/load/parse can dispatch on skill class alone. */
    private record SkillStateCodecBinding<T extends SkillState>(Class<T> stateClass, SkillStateCodec<T> codec) {}

    @Override
    public void onDisable() {
        registeredSkillsNames.clear();
        registeredNamesSkills.clear();
        upgradeParsers.clear();
        stateCodecs.clear();
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
            throw new IllegalArgumentException(clazz.getName() + " does not implement Skill");
        }
        String skillName = getSkillName(clazz);
        if (skillName == null) {
            throw new IllegalArgumentException(clazz.getName() + " is not annotated with @SkillName");
        }
        if (registeredNamesSkills.containsKey(skillName)) {
            throw new IllegalArgumentException(
                    "A skill is already registered under the name '" + skillName + "' (was: "
                            + registeredNamesSkills.get(skillName).getName() + ", attempted: " + clazz.getName() + ")");
        }
        if (registeredSkillsNames.containsKey(clazz)) {
            throw new IllegalArgumentException(clazz.getName() + " is already registered");
        }
        registeredSkillsNames.put(clazz, skillName);
        registeredNamesSkills.put(skillName, clazz);
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
        return AnnotationLookup.findName(clazz, SkillName.class, Skill.class, SkillName::value);
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
     * class must have a public constructor accepting a single {@link Pet} parameter.
     *
     * @param clazz the skill implementation class
     * @param pet the pet that will own the skill instance
     * @return the new skill instance, or {@code null} if instantiation fails
     */
    public Skill getNewSkillInstance(Class<? extends Skill> clazz, Pet pet) {
        if (clazz == null) {
            return null;
        }
        try {
            Constructor<?> ctor = clazz.getConstructor(Pet.class);
            Object obj = ctor.newInstance(pet);
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
     * Parses {@code compound} into the typed {@link SkillState} for
     * {@code skillClass}, or returns {@link Optional#empty()} if no codec is
     * registered, the registered state class doesn't match {@code stateClass},
     * or the codec declines the compound.
     *
     * <p>Called from {@code StoredPet#skillState} on the persisted-pet branch;
     * addons should not call this directly.
     */
    @SuppressWarnings("unchecked")
    public <S extends Skill, T extends SkillState> Optional<T> parseState(
            Class<S> skillClass, Class<T> stateClass, CompoundBinaryTag compound) {
        SkillStateCodecBinding<?> codecBinding = stateCodecs.get(skillClass);
        if (codecBinding == null || !stateClass.equals(codecBinding.stateClass())) {
            return Optional.empty();
        }
        return ((SkillStateCodecBinding<T>) codecBinding).codec().read(compound);
    }

    /**
     * Registers a typed {@link SkillStateCodec} for {@code skillClass}. The
     * codec owns both directions of the NBT round-trip for the skill's
     * persisted state.
     *
     * <p>Each skill class may register at most one codec; re-registration is
     * a programming error.
     *
     * @throws IllegalArgumentException if a codec is already registered for
     *         {@code skillClass}
     */
    public <S extends Skill, T extends SkillState> void registerCodec(
            Class<S> skillClass, Class<T> stateClass, SkillStateCodec<T> codec) {
        if (stateCodecs.containsKey(skillClass)) {
            throw new IllegalArgumentException("A SkillStateCodec is already registered for " + skillClass.getName());
        }
        stateCodecs.put(skillClass, new SkillStateCodecBinding<>(stateClass, codec));
    }

    /**
     * Serializes a skill's runtime state into NBT via its registered
     * {@link SkillStateCodec} (driven by {@link Skill#getState()}). Returns
     * {@code null} if no codec is registered or the codec declines to
     * produce a compound.
     *
     * <p>Centralized so every save site (active pet info, repository
     * serialization, pet-type change) goes through the same dispatch.
     */
    public CompoundBinaryTag saveSkillState(Skill skill) {
        return saveViaCodec(skill);
    }

    /**
     * Restores a skill's runtime state from NBT via its registered
     * {@link SkillStateCodec} (read, then {@link Skill#applyState(SkillState)}).
     * No-op if no codec is registered for the skill.
     */
    public void loadSkillState(Skill skill, CompoundBinaryTag compound) {
        loadViaCodec(skill, compound);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private CompoundBinaryTag saveViaCodec(Skill skill) {
        SkillStateCodecBinding<?> binding = stateCodecs.get(findCodecKey(skill.getClass()));
        if (binding == null) {
            return null;
        }
        Optional<? extends SkillState> state = skill.getState();
        if (state.isEmpty() || !binding.stateClass().isInstance(state.get())) {
            return null;
        }
        return ((SkillStateCodec) binding.codec()).write(state.get());
    }

    @SuppressWarnings("unchecked")
    private boolean loadViaCodec(Skill skill, CompoundBinaryTag compound) {
        Class<? extends Skill> key = findCodecKey(skill.getClass());
        SkillStateCodecBinding<?> binding = stateCodecs.get(key);
        if (binding == null) {
            return false;
        }
        Optional<? extends SkillState> state =
                ((SkillStateCodecBinding<SkillState>) binding).codec().read(compound);
        state.ifPresent(skill::applyState);
        return true;
    }

    private Class<? extends Skill> findCodecKey(Class<?> clazz) {
        while (clazz != null && clazz != Object.class) {
            if (Skill.class.isAssignableFrom(clazz) && stateCodecs.containsKey(clazz)) {
                return clazz.asSubclass(Skill.class);
            }
            for (Class<?> iface : clazz.getInterfaces()) {
                if (Skill.class.isAssignableFrom(iface) && stateCodecs.containsKey(iface)) {
                    return iface.asSubclass(Skill.class);
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }
}
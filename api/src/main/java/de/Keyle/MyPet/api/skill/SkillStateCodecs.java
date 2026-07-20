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

import de.Keyle.MyPet.api.skill.skilltree.Skill;
import de.Keyle.MyPet.api.util.AnnotationLookup;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Self-registering handle pairing a skill's {@link SkillStateCodec} with its
 * state class, declared as a static field on the skill's impl class. Each
 * codec is the single source of truth for its skill's NBT keys; stateless
 * skills carry no codec.
 */
public final class SkillStateCodecs {

    private static final Map<String, SkillStateCodecs> REGISTRY = new ConcurrentHashMap<>();

    private final Class<? extends Skill> skillClass;
    private final Class<? extends SkillState> stateClass;
    private final SkillStateCodec<?> codec;

    private SkillStateCodecs(Class<? extends Skill> skillClass, Class<? extends SkillState> stateClass, SkillStateCodec<?> codec) {
        this.skillClass = skillClass;
        this.stateClass = stateClass;
        this.codec = codec;
    }

    /**
     * Declares a skill's state codec. Self-registers; picked up by
     * {@code SkillManager.registerSkill} when the declaring class is initialized.
     */
    public static <S extends Skill, T extends SkillState> SkillStateCodecs of(
            Class<S> skillClass, Class<T> stateClass, SkillStateCodec<T> codec) {
        Objects.requireNonNull(skillClass, "skillClass");
        Objects.requireNonNull(stateClass, "stateClass");
        Objects.requireNonNull(codec, "codec");
        String name = AnnotationLookup.findName(skillClass, SkillName.class, Skill.class, SkillName::value);
        if (name == null) {
            throw new IllegalArgumentException(skillClass.getName() + " is not annotated with @SkillName");
        }
        SkillStateCodecs handle = new SkillStateCodecs(skillClass, stateClass, codec);
        REGISTRY.put(name.toLowerCase(Locale.ROOT), handle);
        return handle;
    }

    /** Declared handle for a skill name, or null. Case-insensitive. */
    static SkillStateCodecs resolve(String skillName) {
        return REGISTRY.get(skillName.toLowerCase(Locale.ROOT));
    }

    Class<? extends Skill> skillClass() { return skillClass; }

    Class<? extends SkillState> stateClass() { return stateClass; }

    SkillStateCodec<?> codec() { return codec; }
}

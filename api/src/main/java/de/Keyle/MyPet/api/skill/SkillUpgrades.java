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
 * Self-registering handle pairing a skill's {@link UpgradeSchema} with its
 * {@link UpgradeParser}, declared as a static field on the skill's impl class.
 */
public final class SkillUpgrades {

    private static final Map<String, SkillUpgrades> REGISTRY = new ConcurrentHashMap<>();

    private final UpgradeSchema schema;
    private final UpgradeParser<?> parser;

    private SkillUpgrades(UpgradeSchema schema, UpgradeParser<?> parser) {
        this.schema = schema;
        this.parser = parser;
    }

    /**
     * Declares a skill's upgrade schema and parser. Self-registers; picked up by
     * {@code SkillManager.registerSkill} when the declaring class is initialized.
     */
    public static <S extends Skill> SkillUpgrades of(Class<S> skillClass, UpgradeSchema schema, UpgradeParser<S> parser) {
        Objects.requireNonNull(schema, "schema");
        Objects.requireNonNull(parser, "parser");
        String name = AnnotationLookup.findName(skillClass, SkillName.class, Skill.class, SkillName::value);
        if (name == null) {
            throw new IllegalArgumentException(skillClass.getName() + " is not annotated with @SkillName");
        }
        SkillUpgrades handle = new SkillUpgrades(schema, parser);
        REGISTRY.put(name.toLowerCase(Locale.ROOT), handle);
        return handle;
    }

    /** Declared handle for a skill name, or null. Case-insensitive. */
    static SkillUpgrades resolve(String skillName) {
        return REGISTRY.get(skillName.toLowerCase(Locale.ROOT));
    }

    UpgradeSchema schema() { return schema; }

    UpgradeParser<?> parser() { return parser; }
}

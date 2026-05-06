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

import java.lang.annotation.*;

/**
 * Annotation that declares the canonical name of a
 * {@link de.Keyle.MyPet.api.skill.skilltree.Skill} implementation. The
 * {@link SkillManager} uses this annotation (walking superclasses and interfaces)
 * to register and look up skill classes by name.
 *
 * <p>Every concrete skill implementation must carry this annotation (directly or
 * inherited from a parent interface/class). The {@link #value()} is used as the
 * key in skilltree JSON files and in the {@link Skills} name-based lookup.
 *
 * <p>Example usage:
 * <pre>{@code
 * @SkillName("Damage")
 * public class DamageSkill extends AbstractSkill { ... }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface SkillName {

    /**
     * The unique skill identifier used in skilltree files and registration.
     */
    String value();

    /**
     * Optional translation key for the skill's display name. When empty, the
     * plugin falls back to a default translation lookup based on {@link #value()}.
     */
    String translationNode() default "";
}
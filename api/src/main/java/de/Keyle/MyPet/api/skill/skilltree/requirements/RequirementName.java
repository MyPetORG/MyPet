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

package de.Keyle.MyPet.api.skill.skilltree.requirements;

import java.lang.annotation.*;

/**
 * Annotation that assigns a unique name to a {@link Requirement} implementation.
 *
 * <p>The {@link #value()} is used as the lookup key when skilltree JSON files reference a
 * requirement by name. The optional {@link #translationNode()} allows the requirement name
 * to be localized for display in player-facing UIs.
 *
 * <p>This annotation is inherited, so subclasses of annotated requirement classes will
 * share the same name unless they override it with their own annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface RequirementName {

    /** The unique identifier for this requirement (used in {@code .st.json} files). */
    String value();

    /** Optional translation node for localizing the requirement name in player UIs. */
    String translationNode() default "";
}
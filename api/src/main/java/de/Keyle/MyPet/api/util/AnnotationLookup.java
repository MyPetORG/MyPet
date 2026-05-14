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

package de.Keyle.MyPet.api.util;

import java.lang.annotation.Annotation;
import java.util.function.Function;

/**
 * Resolve an annotation-carried name (e.g. {@code @SkillName("Damage")},
 * {@code @LeashFlagName("BelowHP")}, {@code @RequirementName("PetLevel")})
 * by walking a class's superclass chain and implemented interfaces.
 *
 * <p>The annotation is only honored on classes assignable to a given
 * marker type — e.g. {@code @SkillName} on a class that doesn't implement
 * {@code Skill} is ignored. This matches the historical behavior of the
 * three near-identical walkers that lived on
 * {@code SkillManager#getSkillName}, {@code LeashFlagManager#getLeashFlagName},
 * and {@code SkilltreeManager#getRequirementName}.
 */
public final class AnnotationLookup {

    private AnnotationLookup() {
    }

    /**
     * Recursively search {@code clazz}, its superclass chain, and every
     * implemented interface for an instance of {@code annotationType}
     * carried on a class assignable to {@code markerType}. The first match
     * wins.
     *
     * @param clazz          the class to inspect (or any ancestor reached
     *                       during recursion)
     * @param annotationType the annotation class to look for
     * @param markerType     the marker interface/class the annotated type
     *                       must extend or implement
     * @param nameExtractor  pulls the {@code value()} (or equivalent) off
     *                       the annotation instance
     * @return the extracted name, or {@code null} if no annotated ancestor
     *         is found
     */
    public static <A extends Annotation> String findName(
            Class<?> clazz,
            Class<A> annotationType,
            Class<?> markerType,
            Function<A, String> nameExtractor) {
        if (clazz == null || clazz == Object.class) {
            return null;
        }
        if (markerType.isAssignableFrom(clazz)) {
            A annotation = clazz.getAnnotation(annotationType);
            if (annotation != null) {
                return nameExtractor.apply(annotation);
            }
        }
        String fromSuper = findName(clazz.getSuperclass(), annotationType, markerType, nameExtractor);
        if (fromSuper != null) {
            return fromSuper;
        }
        for (Class<?> iface : clazz.getInterfaces()) {
            String fromIface = findName(iface, annotationType, markerType, nameExtractor);
            if (fromIface != null) {
                return fromIface;
            }
        }
        return null;
    }
}

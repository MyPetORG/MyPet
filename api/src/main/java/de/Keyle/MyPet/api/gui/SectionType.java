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

package de.Keyle.MyPet.api.gui;

/**
 * Pairs a section's type id with its concrete record class, renderer, and codec.
 * Built-in types are static fields on type-holder classes that call
 * {@link #register} from a static initializer; the registry forces those classes
 * to load at plugin OnLoad.
 */
public record SectionType<S extends Section>(
    String id,
    Class<S> sectionClass,
    SectionRenderer<S> renderer,
    JsonCodec<S> codec
) {
    /** Construct AND register. Returns the same instance for assignment to a static field. */
    public static <S extends Section> SectionType<S> register(
            String id, Class<S> sectionClass,
            SectionRenderer<S> renderer, JsonCodec<S> codec) {
        SectionType<S> t = new SectionType<>(id, sectionClass, renderer, codec);
        SectionTypeRegistry.register(t);
        return t;
    }
}

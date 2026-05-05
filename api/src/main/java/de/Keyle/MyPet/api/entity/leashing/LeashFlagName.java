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

package de.Keyle.MyPet.api.entity.leashing;

import java.lang.annotation.*;

/**
 * Declares the config-facing name of a {@link LeashFlag} implementation.
 * The {@link LeashFlagManager} uses this annotation (walking the class
 * hierarchy if needed) to map between the string names used in skilltree
 * YAML and live flag instances.
 * <p>
 * Marked {@link Inherited} so that subclasses of a flag (e.g.,
 * integration-specific overrides) inherit the parent's name unless they
 * re-declare their own.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface LeashFlagName {

    /**
     * The config/skilltree name for this flag (e.g. {@code "Baby"}, {@code "WorldGuard"}).
     */
    String value();

    /**
     * Optional translation node for the player-facing label shown in
     * {@code /petinfo} and leash-failure messages. When empty (default),
     * the system falls back to a generated key based on {@link #value()}.
     */
    String translationNode() default "";
}
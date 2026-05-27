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

package de.Keyle.MyPet.api.util.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link ServiceContainer} that bridges to a specific third-party Bukkit plugin.
 * The {@link ServiceManager} only instantiates the service if {@link #value()} is installed
 * on the server, and only registers it if the per-plugin {@code <name>.Enabled} flag in
 * {@code hooks-config.yml} is {@code true}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RequiresPlugin {
    /**
     * @return the Bukkit plugin name to gate on
     */
    String value();

    /**
     * @return optional fully-qualified main-class name; if non-empty, the installed plugin
     *         must match this class. Used when multiple plugins share a name.
     */
    String classPath() default "";
}

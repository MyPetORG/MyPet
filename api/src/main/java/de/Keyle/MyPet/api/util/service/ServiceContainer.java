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

package de.Keyle.MyPet.api.util.service;

import de.Keyle.MyPet.api.Util;
import org.bukkit.event.Listener;

/**
 * Services are a way to register possible functionallity
 */
public interface ServiceContainer extends Listener {
    /**
     * Enables the service and returns if activation was successfull
     *
     * @return if activation was successfull
     */
    default boolean onEnable() {
        return true;
    }

    /**
     * Disables the service
     */
    default void onDisable() {
    }

    /**
     * Returns the name of the service
     *
     * @return name of the service
     */
    default String getServiceName() {
        ServiceName sn = Util.getClassAnnotation(this.getClass(), ServiceName.class);
        if (sn != null) {
            return sn.value();
        }
        return getClass().getName();
    }
}
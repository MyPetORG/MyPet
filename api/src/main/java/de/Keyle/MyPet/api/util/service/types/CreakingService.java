/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2020 Keyle
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

package de.Keyle.MyPet.api.util.service.types;

import de.Keyle.MyPet.api.util.service.ServiceContainer;
import de.Keyle.MyPet.api.util.service.ServiceName;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

/**
 * Service for accessing Creaking entity functionality.
 * This abstraction allows version-specific implementations without reflection.
 */
@ServiceName("CreakingService")
public abstract class CreakingService implements ServiceContainer {

    /**
     * Gets the home location (Creaking Heart position) of a Creaking entity.
     *
     * @param entity The entity to get the home location from
     * @return The home Location, or null if not a Creaking or not heart-linked
     */
    public abstract Location getCreakingHome(Entity entity);

    /**
     * Checks if this version supports Creaking Heart capture.
     *
     * @return true if Creaking Heart capture is supported
     */
    public abstract boolean isSupported();
}

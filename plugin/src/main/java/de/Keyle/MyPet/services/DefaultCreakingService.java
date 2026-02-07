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

package de.Keyle.MyPet.services;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.service.types.CreakingService;
import org.bukkit.Location;
import org.bukkit.entity.Creaking;
import org.bukkit.entity.Entity;

/**
 * Default implementation of CreakingService.
 * Uses the Bukkit Creaking API directly (available in 1.21.4+).
 * The Creaking class reference is isolated in a static inner class to prevent
 * class loading errors on servers older than 1.21.4.
 */
public class DefaultCreakingService extends CreakingService {

    private final boolean supported;

    public DefaultCreakingService() {
        // Check if we're on 1.21.4+ where Creaking exists
        this.supported = MyPetApi.getCompatUtil().compareWithMinecraftVersion("1.21.4") >= 0;
    }

    @Override
    public Location getCreakingHome(Entity entity) {
        if (!supported) {
            return null;
        }
        // Delegate to inner class to isolate Creaking class reference
        return CreakingHelper.getHome(entity);
    }

    @Override
    public boolean isSupported() {
        return supported;
    }

    @Override
    public boolean onEnable() {
        return true;
    }

    @Override
    public void onDisable() {
    }

    /**
     * Inner class that references the Creaking class.
     * This class is only loaded when getHome() is called, which only happens
     * on 1.21.4+ servers where Creaking exists.
     */
    private static class CreakingHelper {
        static Location getHome(Entity entity) {
            if (entity instanceof Creaking) {
                return ((Creaking) entity).getHome();
            }
            return null;
        }
    }
}

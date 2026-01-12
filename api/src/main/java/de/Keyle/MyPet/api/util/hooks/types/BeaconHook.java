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

package de.Keyle.MyPet.api.util.hooks.types;

import de.Keyle.MyPet.api.util.hooks.PluginHook;
import org.bukkit.Location;

/**
 * This interface defines hooks that control beacon skill behavior in regions
 */
public interface BeaconHook extends PluginHook {

    /**
     * Returns if beacon effects can be activated at a certain location
     *
     * @param location checked location
     * @return if beacon is allowed at this location
     */
    boolean isBeaconAllowed(Location location);

    /**
     * Returns if beacon effects can be shared to non-owners at a certain location
     *
     * @param location checked location
     * @return if beacon sharing is allowed at this location
     */
    boolean isBeaconShareAllowed(Location location);

    /**
     * Returns if owners can receive their own beacon effects at a certain location
     *
     * @param location checked location
     * @return if beacon self-effects are allowed at this location
     */
    boolean isBeaconSelfAllowed(Location location);

    /**
     * Returns the range multiplier for beacons at a certain location
     *
     * @param location checked location
     * @return range multiplier (default 1.0)
     */
    double getBeaconRangeMultiplier(Location location);

    /**
     * Returns the duration multiplier for beacon effects at a certain location
     *
     * @param location checked location
     * @return duration multiplier (default 1.0)
     */
    double getBeaconDurationMultiplier(Location location);

    /**
     * Returns the amplifier modifier for beacon effects at a certain location
     *
     * @param location checked location
     * @return amplifier modifier (default 0)
     */
    int getBeaconAmplifierModifier(Location location);
}

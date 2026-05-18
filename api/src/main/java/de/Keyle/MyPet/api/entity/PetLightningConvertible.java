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

package de.Keyle.MyPet.api.entity;

import de.Keyle.MyPet.api.config.PetConfigLookup;

/**
 * Marker for pet types that vanilla converts to a different species when struck
 * by lightning (Pig → ZombifiedPiglin, Villager → Witch). When admins opt in
 * via {@code MyPet.Pets.<Type>.AllowLightningConversion}, the pet's domain
 * object is re-typed to the new species via
 * {@code PetManager#convertPetType}, preserving UUID, name, XP, skill state,
 * and owner — same flow as {@link PetZombifiable}.
 *
 * <p>The YAML row is auto-registered for every type that implements this marker
 * — adding a new lightning-convertible pet only requires implementing this
 * interface. Default is {@code false}: the pet stays its original type and the
 * lightning bolt's species-conversion arm is suppressed by
 * {@code PetLightningStrikeListener}.
 *
 * <p>The Mooshroom red↔brown variant flip and the Creeper powered-state toggle
 * are separate shapes (no type change) and have their own per-pet flags rather
 * than implementing this marker.
 */
public interface PetLightningConvertible extends Pet {

    default boolean allowLightningConversion() {
        return PetConfigLookup.boolValue(getClass(), "AllowLightningConversion", false);
    }
}

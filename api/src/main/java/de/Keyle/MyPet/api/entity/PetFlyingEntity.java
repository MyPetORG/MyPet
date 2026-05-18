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
 * Marker for pet types whose underlying vanilla mob naturally flies — implies
 * Pet's AI/movement layer should treat this pet as airborne (no gravity,
 * flight pathing, no float goal). Read at runtime by
 * {@link PetType#isFlyingPet()} via {@code Class.isAssignableFrom}.
 *
 * <p>The {@link #canFly()} default reads the per-pet preference from
 * {@code PetConfigKeys.<Pet>.CAN_FLY}, loaded from
 * {@code MyPet.Pets.<Type>.CanFly} in {@code pet-config.yml}. Adding a
 * new flying pet means implementing this interface <em>and</em> adding a
 * {@code CAN_FLY} entry to the matching {@code PetConfigKeys} nested class.
 */
public interface PetFlyingEntity extends Pet {

    default boolean canFly() {
        return PetConfigLookup.boolValue(getClass(), "CanFly", true);
    }
}

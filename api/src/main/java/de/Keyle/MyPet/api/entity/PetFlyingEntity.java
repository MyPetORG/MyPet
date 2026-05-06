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

import de.Keyle.MyPet.api.Configuration;

/**
 * Marker for pet types whose underlying vanilla mob naturally flies — implies
 * Pet's AI/movement layer should treat this pet as airborne (no gravity,
 * flight pathing, no float goal). Read at runtime by
 * {@link PetType#isFlyingPet()} via {@code Class.isAssignableFrom}.
 *
 * <p>The {@link #canFly()} default consults the per-pet preference loaded
 * from {@code MyPet.Pets.<Type>.CanFly} in {@code pet-config.yml}. The YAML
 * row is auto-registered for every type that implements this marker — adding
 * a new flying pet only requires implementing this interface.
 *
 * <p>Extends {@link PetGlidingEntity} because every flying pet must also
 * glide: when an admin disables flight, a ridden pet still needs to slow-fall
 * so the rider doesn't plummet. The inherited {@link #canGlide()} reads its
 * own {@code CanGlide} config row.
 */
public interface PetFlyingEntity extends PetGlidingEntity {

    default boolean canFly() {
        return Configuration.MyPet.canFly(getPetType());
    }
}

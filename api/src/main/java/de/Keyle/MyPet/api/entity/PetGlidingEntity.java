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
 * Marker for pet types that slow-fall instead of dropping like a stone — e.g.,
 * Chicken, or any flying pet whose rider should drift down rather than plummet
 * when flight is disabled. Read at runtime via {@code Class.isAssignableFrom}.
 *
 * <p>The {@link #canGlide()} default consults the per-pet preference loaded
 * from {@code MyPet.Pets.<Type>.CanGlide} in {@code pet-config.yml}. The YAML
 * row is auto-registered for every type that implements this marker.
 *
 * <p>{@link PetFlyingEntity} extends this marker, so every flying pet is
 * also a gliding pet — necessary so a rider on a fly-disabled mount drifts
 * down instead of free-falling.
 */
public interface PetGlidingEntity extends Pet {

    default boolean canGlide() {
        return Configuration.MyPet.canGlide(getPetType());
    }
}

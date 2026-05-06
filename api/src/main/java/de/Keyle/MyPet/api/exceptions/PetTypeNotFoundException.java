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

package de.Keyle.MyPet.api.exceptions;

import de.Keyle.MyPet.api.entity.PetType;

/**
 * Thrown by {@link PetType#byName(String)} and
 * {@link PetType#byEntityTypeName(String)} when
 * the requested name does not match any registered pet type. Callers that
 * need a null-safe lookup should use
 * {@link PetType#byNameOrNull(String)} instead.
 */
public class PetTypeNotFoundException extends RuntimeException {

    /**
     * @param type the unrecognized type name that triggered the lookup failure
     */
    public PetTypeNotFoundException(String type) {
        super(type + " is not a valid MyPet type");
    }
}
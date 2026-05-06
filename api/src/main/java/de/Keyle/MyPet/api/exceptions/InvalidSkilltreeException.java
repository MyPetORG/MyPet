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

package de.Keyle.MyPet.api.exceptions;

/**
 * Thrown when a {@code .st.json} skilltree file fails to parse — malformed
 * JSON, missing required fields, or invalid skill/level references.
 * The message includes the failing section name and the originating line
 * number to aid admin-side debugging without requiring a full stack trace.
 */
public class InvalidSkilltreeException extends RuntimeException {

    /**
     * @param part the skilltree section or filename that failed
     * @param e    the underlying parse/validation error
     */
    public InvalidSkilltreeException(String part, Exception e) {
        super(part + " :: " + e.getMessage() + " :: " + e.getStackTrace()[0].getLineNumber(), e);
    }
}

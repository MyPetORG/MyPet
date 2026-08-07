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

package de.Keyle.MyPet.webeditor;

import java.io.IOException;

/**
 * Thrown when the relay refuses to open a session because the server could not prove it runs a
 * licensed copy of MyPet. Distinguished from a generic transport failure so the command can show
 * a purchase-oriented message instead of a network error.
 */
public class EditorNotEntitledException extends IOException {

    public EditorNotEntitledException(String message) {
        super(message);
    }
}

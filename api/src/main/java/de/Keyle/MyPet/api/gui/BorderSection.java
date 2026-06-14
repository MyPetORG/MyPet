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

package de.Keyle.MyPet.api.gui;

/** Outer-ring decoration. {@code thickness} 1 = single ring, 2 = two-deep, etc. */
public record BorderSection(
    String id,
    SectionType<BorderSection> type,
    int thickness,
    ItemAppearance item
) implements Section {

    public BorderSection {
        if (thickness < 1) throw new IllegalArgumentException("BorderSection '" + id + "': thickness must be >= 1");
        if (item == null) throw new IllegalArgumentException("BorderSection '" + id + "': item is required");
    }
}

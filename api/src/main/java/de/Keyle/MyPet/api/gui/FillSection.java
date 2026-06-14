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

import org.jetbrains.annotations.Nullable;

/**
 * Backdrop fill. {@code region} null = whole menu. Renders before all other sections.
 * Other sections overwrite its slots.
 */
public record FillSection(
    String id,
    SectionType<FillSection> type,
    @Nullable Region region,
    ItemAppearance item
) implements Section {

    public FillSection {
        if (item == null) throw new IllegalArgumentException("FillSection '" + id + "': item is required");
    }

    public record Region(int col, int row, int width, int height) {
        public Region {
            if (width <= 0 || height <= 0) throw new IllegalArgumentException("FillSection.Region: width/height must be > 0");
        }
    }
}

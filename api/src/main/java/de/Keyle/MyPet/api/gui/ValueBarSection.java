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

/**
 * One-row horizontal slider. Cells {@code [0..position]} render with
 * {@code highItem}, cells {@code (position..width-1]} with {@code lowItem}.
 * The handler supplies the fill position via {@link MenuHandler#valueBarPosition}.
 * Each cell exposes a {@code <percent>} placeholder.
 */
public record ValueBarSection(
    String id,
    SectionType<ValueBarSection> type,
    int col,
    int row,
    int width,
    ItemAppearance lowItem,
    ItemAppearance highItem,
    SoundSpec soundOnClick
) implements Section {

    public ValueBarSection {
        if (width <= 0 || width > 9) {
            throw new IllegalArgumentException("ValueBarSection '" + id + "': width must be 1..9, was " + width);
        }
        if (lowItem == null || highItem == null) {
            throw new IllegalArgumentException("ValueBarSection '" + id + "': low-item and high-item are required");
        }
        if (soundOnClick == null) soundOnClick = SoundSpec.Silent.INSTANCE;
    }

    public int percentAt(int index) {
        if (width <= 1) return 100;
        return Math.round((float) index / (width - 1) * 100f);
    }
}

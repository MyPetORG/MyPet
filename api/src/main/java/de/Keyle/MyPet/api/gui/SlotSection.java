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

import java.util.Map;

/**
 * Single-slot section. Either a fixed appearance ({@link #item} non-null) or a
 * multi-state slot ({@link #states} non-null with {@link #defaultState} naming
 * a key from the map). Exactly one of the two must be set.
 *
 * <p>{@code hideAtBoundary} only matters when this slot is referenced by a
 * {@link PaginatedListSection} as {@code previous-page-section} or
 * {@code next-page-section}: when true (default), the button is not rendered
 * if the list is on the first/last page (no previous/next page to go to).
 * When false, the button is always rendered (clicking it at the boundary is a
 * silent no-op).
 */
public record SlotSection(
    String id,
    SectionType<SlotSection> type,
    int col,
    int row,
    @Nullable ItemAppearance item,
    @Nullable Map<String, ItemAppearance> states,
    @Nullable String defaultState,
    SoundSpec soundOnClick,
    boolean hideAtBoundary
) implements Section {

    public SlotSection {
        if ((item == null) == (states == null)) {
            throw new IllegalArgumentException("SlotSection '" + id + "': exactly one of `item` or `states` must be set");
        }
        if (states != null) {
            states = Map.copyOf(states);
            if (defaultState == null || !states.containsKey(defaultState)) {
                throw new IllegalArgumentException("SlotSection '" + id + "': default-state must reference a defined state");
            }
        }
        if (soundOnClick == null) soundOnClick = SoundSpec.Silent.INSTANCE;
    }
}

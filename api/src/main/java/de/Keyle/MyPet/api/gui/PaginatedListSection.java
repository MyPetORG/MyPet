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
 * Rectangular region populated by a per-item template applied to a stream supplied
 * by the menu handler. {@code previousPageSectionId} / {@code nextPageSectionId}
 * are optional: when both are null the list does not paginate and any items past
 * {@link #slotCapacity()} are dropped (useful for bounded item sets like beacon buffs).
 */
public record PaginatedListSection(
    String id,
    SectionType<PaginatedListSection> type,
    int col,
    int row,
    int width,
    int height,
    ItemAppearance template,
    @Nullable String previousPageSectionId,
    @Nullable String nextPageSectionId,
    SoundSpec soundOnPageChange,
    SoundSpec soundOnTemplateClick
) implements Section {

    public PaginatedListSection {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("PaginatedListSection '" + id + "': width/height must be > 0");
        }
        if (template == null) throw new IllegalArgumentException("PaginatedListSection '" + id + "': template is required");
        if (soundOnPageChange == null) soundOnPageChange = SoundSpec.Silent.INSTANCE;
        if (soundOnTemplateClick == null) soundOnTemplateClick = SoundSpec.Silent.INSTANCE;
    }

    public int slotCapacity() { return width * height; }

    /** True if both page-button refs are supplied — the list paginates on overflow. */
    public boolean isPaginated() {
        return previousPageSectionId != null && nextPageSectionId != null;
    }
}

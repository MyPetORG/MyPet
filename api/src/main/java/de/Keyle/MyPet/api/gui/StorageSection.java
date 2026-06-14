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
 * Free-input slots backed by an ItemStack[] supplied by the menu handler at open time
 * and persisted via the handler at close time. Drag/drop inside the region works
 * like a vanilla chest; outside is cancelled.
 */
public record StorageSection(
    String id,
    SectionType<StorageSection> type,
    int col,
    int row,
    int width,
    int height,
    String storageId
) implements Section {

    public StorageSection {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("StorageSection '" + id + "': width/height must be > 0");
        }
        if (storageId == null || storageId.isBlank()) {
            throw new IllegalArgumentException("StorageSection '" + id + "': storageId is required");
        }
    }

    public int slotCapacity() { return width * height; }
}

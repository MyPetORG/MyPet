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

import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

/**
 * Payload routed to {@link MenuHandler#onClick} for every click inside a tracked menu.
 * `itemIndex` is the index inside a paginated-list section (or -1 for non-list sections).
 */
public record ClickPayload(int slot, int itemIndex, ClickType clickType, boolean shift, ItemStack cursor) {
    public static ClickPayload forSlot(int slot, ClickType type, boolean shift, ItemStack cursor) {
        return new ClickPayload(slot, -1, type, shift, cursor);
    }
}

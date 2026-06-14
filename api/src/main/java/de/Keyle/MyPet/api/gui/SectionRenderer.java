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

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * Interface a section type implements to render itself into a Bukkit {@link Inventory}
 * and respond to clicks on the slots it owns.
 */
public interface SectionRenderer<S extends Section> {

    /** Render this section's items into the inventory. Called on open and on refresh. */
    void render(S section, Inventory inv, RenderContext ctx);

    /** Handle a click on a slot owned by this section. */
    ClickResult onClick(S section, ClickPayload payload, RenderContext ctx);

    /** Called once when the owning {@link MenuInstance} is closing. */
    default void onClose(S section, CloseReason reason, RenderContext ctx) {}

    /** Slot indices (0..rows*9-1) this section renders into in the given context. */
    java.util.Set<Integer> ownedSlots(S section);

    /**
     * Decorative sections (border, fill) paint slots but yield rendering to non-decorative
     * sections that also claim them. The overlap validator skips decorative sections.
     */
    default boolean decorative() { return false; }

    enum ClickResult { NO_OP, DELEGATE_TO_HANDLER, REFRESH_SECTION, CLOSE }
}

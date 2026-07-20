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

/**
 * Runtime handle for one open menu. Handlers receive this in their callbacks and use
 * it to mutate the visible state (refresh sections, flip slot states, pop back).
 */
public interface MenuInstance {

    MenuDefinition definition();
    Player viewer();

    /** Re-render one section. */
    void refreshSection(String sectionId);

    /** Unconditional close — fires {@link MenuHandler#onClose} with {@link CloseReason#PLUGIN_CLOSED}; stack cleared. */
    void close();

    /**
     * ESC-as-back from a click handler.
     * @return true if a pop happened, false if no-op (menu lacks {@code esc-supports-back} or stack empty)
     */
    boolean popBack();

    /** Fire a one-off sound at the viewer (for ad-hoc feedback like error pings). */
    void playSound(SoundSpec spec);

    /** Typed access to a section by id; throws if id is unknown or type mismatches. */
    <S extends Section> S section(String id, Class<S> type);

    /** Flip a multi-state {@link SlotSection}'s state and refresh that section. */
    void setSlotState(String slotSectionId, String stateName);

    /** Current state name of a multi-state slot section, or its default. */
    String getSlotState(String slotSectionId);

    /** The context object this menu was opened with. */
    Object context();
}

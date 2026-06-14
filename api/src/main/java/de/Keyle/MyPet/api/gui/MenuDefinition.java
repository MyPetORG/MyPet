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

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Immutable resolved blueprint for one menu. Produced by the loader after
 * deep-merging the bundled and overlay JSON. The renderer walks this to produce
 * the live inventory.
 */
public record MenuDefinition(
    String menuId,
    String titleMiniMessage,
    int rows,
    boolean escSupportsBack,
    SoundSpec soundOnOpen,
    SoundSpec soundOnClose,
    @Nullable SoundSpec soundOnBack,
    Map<String, Section> sections
) {
    public MenuDefinition {
        if (rows < 1 || rows > 6) throw new IllegalArgumentException("rows must be 1..6, was " + rows);
        if (titleMiniMessage == null) throw new IllegalArgumentException("title is required");
        if (sections == null) throw new IllegalArgumentException("sections is required");
        sections = Map.copyOf(sections);
        if (soundOnOpen == null)  soundOnOpen  = SoundSpec.Silent.INSTANCE;
        if (soundOnClose == null) soundOnClose = SoundSpec.Silent.INSTANCE;
        // soundOnBack: null is the "fall back to soundOnClose" sentinel; preserve it.
    }

    /** Effective sound when popping back: explicit {@code soundOnBack} or {@code soundOnClose}. */
    public SoundSpec effectiveSoundOnBack() {
        return soundOnBack != null ? soundOnBack : soundOnClose;
    }

    public int slotCount() { return rows * 9; }
}

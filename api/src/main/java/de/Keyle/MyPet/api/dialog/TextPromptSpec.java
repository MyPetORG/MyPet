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

package de.Keyle.MyPet.api.dialog;

import net.kyori.adventure.text.Component;

/**
 * Parameters for a text-input prompt opened via {@link DialogService#promptText}.
 * The compact constructor supplies defaults so callers can pass {@code null} for
 * any optional field.
 */
public record TextPromptSpec(Component title, Component prompt, String initialValue, int maxLength) {
    public TextPromptSpec {
        if (title == null) title = Component.empty();
        if (prompt == null) prompt = Component.empty();
        if (initialValue == null) initialValue = "";
        if (maxLength <= 0) maxLength = 32;
    }
}

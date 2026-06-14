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
 * Typed identity of a registered menu. `id` is the JSON file basename (e.g. "pet-selection");
 * `contextType` lets {@link GuiService#openMenu} type-check the caller's context.
 */
public record MenuId<C>(String id, Class<C> contextType) {
    public static <C> MenuId<C> of(String id, Class<C> type) { return new MenuId<>(id, type); }
}

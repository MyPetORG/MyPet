/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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

package de.Keyle.MyPet.compat.v1_12_R1.util.iconmenu;

import de.Keyle.MyPet.api.util.Compat;

/**
 * Version-specific IconMenuInventory for Minecraft v1_12_R1.
 * <p>
 * This class inherits all GUI logic from the API-level {@code IconMenuInventory}
 * <p>
 * Intended behavior:
 * <ul>
 *   <li>No NMS usage</li>
 *   <li>Defer all functionality to the shared parent implementation</li>
 * </ul>
 */
@Compat("v1_12_R1")
public class IconMenuInventory extends de.Keyle.MyPet.api.gui.IconMenuInventory {

}

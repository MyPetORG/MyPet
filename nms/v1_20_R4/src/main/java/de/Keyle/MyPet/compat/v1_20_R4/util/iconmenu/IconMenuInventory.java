/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2020 Keyle
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

package de.Keyle.MyPet.compat.v1_20_R4.util.iconmenu;

import de.Keyle.MyPet.api.util.Compat;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Version-specific IconMenuInventory for Minecraft v1_20_R4
 * <p>
 * This class inherits all GUI logic from the API-level {@code IconMenuInventory}
 * and only overrides {@link #addGlint(ItemMeta)} to use the native modern glow API.
 * <p>
 * Intended behavior:
 * <ul>
 *   <li>No NMS usage</li>
 *   <li>Defer most functionality to the shared parent implementation</li>
 *   <li>Use modern glint override</li>
 * </ul>
 */
@Compat("v1_20_R4")
public class IconMenuInventory extends de.Keyle.MyPet.api.gui.IconMenuInventory {
    /**
     * Apply a glow effect using the modern server API when available on 1.20.4+,
     * bypassing the dummy-enchantment fallback used on legacy versions.
     *
     * @param meta ItemMeta to modify
     * @return ItemMeta with glow flag applied
     */
    @Override
    protected ItemMeta addGlint(ItemMeta meta) {
        // Modern Spigot/Paper API call; safe on 1.20.4+ environments
        meta.setEnchantmentGlintOverride(true);
        return meta;
    }
}

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

import org.bukkit.Color;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Pure visual description of one item slot. Title and lore are MiniMessage strings,
 * pre-render; placeholders are applied with the active {@link PlaceholderCatalog}.
 */
public record ItemAppearance(
    Material material,
    String title,
    List<String> lore,
    boolean glow,
    int amount,
    int customModelData,
    HeadSkin headSkin,
    @Nullable Color potionColor
) {
    public ItemAppearance {
        if (material == null) throw new IllegalArgumentException("material is required");
        if (amount < 1) throw new IllegalArgumentException("amount must be >= 1, was " + amount);
        lore = lore == null ? List.of() : List.copyOf(lore);
        if (headSkin == null) headSkin = HeadSkin.STEVE;
    }
}

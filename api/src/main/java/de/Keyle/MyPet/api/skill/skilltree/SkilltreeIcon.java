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

package de.Keyle.MyPet.api.skill.skilltree;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.bukkit.Material;

/**
 * Represents the visual icon displayed for a {@link Skilltree} in inventory-based selection GUIs.
 *
 * <p>The icon is defined by a Bukkit {@link Material} name and an optional glowing enchantment
 * effect. Both properties support Lombok's fluent chaining (via {@code @Accessors(chain = true)}).
 *
 * <p>Defaults to an oak sapling ({@link Material#OAK_SAPLING}) with no glow.
 */
public class SkilltreeIcon {
    @Getter
    @Setter
    @Accessors(chain = true)
    @NonNull
    protected String material = Material.OAK_SAPLING.name();
    @Getter
    @Setter
    @Accessors(chain = true)
    protected boolean glowing = false;
}

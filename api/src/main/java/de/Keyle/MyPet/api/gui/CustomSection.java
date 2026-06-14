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

import com.google.gson.JsonObject;

/**
 * Generic carrier for sections whose type is registered at runtime (e.g. a `value-bar`
 * widget). The codec for the registered type decodes/encodes {@link #raw} into a
 * concrete record from its own package and stores a strongly-typed view inside
 * {@link MenuDefinition} as needed; the carrier itself just holds the parsed JSON
 * payload and the type handle until then.
 */
public record CustomSection(
    String id,
    SectionType<CustomSection> type,
    JsonObject raw
) implements Section {

    public CustomSection {
        if (raw == null) throw new IllegalArgumentException("CustomSection '" + id + "': raw is required");
    }
}

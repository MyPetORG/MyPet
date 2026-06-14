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
 * JSON ↔ section-record bridge. {@link #decode} parses a section's JSON into the
 * concrete record; {@link #encode} is provided for the future web editor (round-trip).
 */
public interface JsonCodec<S extends Section> {
    /**
     * @param id     section id from the parent map key
     * @param raw    the section JSON object (after sparse-merge with the bundled defaults)
     * @param ctx    validation/parsing context (menu id, rows, sibling section names, etc.)
     * @return the decoded section record
     * @throws ValidationException on any structural / semantic error
     */
    S decode(String id, JsonObject raw, CodecContext ctx);

    JsonObject encode(S section);
}

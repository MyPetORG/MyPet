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

package de.Keyle.MyPet.api.util.configuration.settings;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * One token parsed out of a {@link Settings} string. Either a keyed entry
 * (e.g. {@code "min=20"} — key {@code "min"}, value {@code "20"}) or a
 * positional entry (e.g. {@code "50%"} — key {@code "50%"}, value absent).
 *
 * <p>Prefer the typed accessors ({@link #getInt()}, {@link #getDouble()},
 * {@link #getBoolean()}). {@link #asString()} is kept as an escape hatch for
 * values that carry custom syntax the typed accessors don't model (e.g. a
 * {@code %} suffix).
 */
public class Setting {

    private final String key;
    private final String value;

    /** Positional entry. */
    public Setting(String key) {
        this.key = key;
        this.value = null;
    }

    /** Keyed entry parsed from {@code k=v}. */
    public Setting(String key, String value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Original-case key. For a keyed entry this is the LHS of {@code k=v};
     * for a positional entry this is the whole token.
     */
    public String getKey() {
        return key;
    }

    /**
     * Raw string content of this entry. For a keyed entry returns the
     * value; for a positional entry returns the key. Use this when the
     * typed accessors don't fit (custom syntax, suffix detection, etc.).
     */
    public String asString() {
        return value != null ? value : key;
    }

    /** Parse {@link #asString()} as an int. */
    public OptionalInt getInt() {
        try {
            return OptionalInt.of(Integer.parseInt(asString().trim()));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    /** Parse {@link #asString()} as a double. */
    public OptionalDouble getDouble() {
        try {
            return OptionalDouble.of(Double.parseDouble(asString().trim()));
        } catch (NumberFormatException e) {
            return OptionalDouble.empty();
        }
    }

    /** Parse {@link #asString()} as a boolean. Accepts only {@code "true"}/{@code "false"} (case-insensitive). */
    public Optional<Boolean> getBoolean() {
        String s = asString().trim();
        if (s.equalsIgnoreCase("true")) return Optional.of(true);
        if (s.equalsIgnoreCase("false")) return Optional.of(false);
        return Optional.empty();
    }
}

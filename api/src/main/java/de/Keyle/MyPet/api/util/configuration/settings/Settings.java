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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Parsed key=value bag from a Requirement or LeashFlag string like
 * {@code "MinHealth:min=20:max=50"}. Typed accessors do the parsing so
 * callers never touch raw strings or write their own try/catch.
 *
 * <p>The parser distinguishes two token shapes:
 * <ul>
 *   <li><b>Keyed</b> ({@code k=v}, e.g. {@code "min=20"}) — reachable by
 *       case-insensitive name via {@link #getInt(String)},
 *       {@link #getDouble(String)}, {@link #getString(String)},
 *       {@link #getBoolean(String)}, {@link #contains(String)},
 *       {@link #get(String)}.</li>
 *   <li><b>Positional</b> (no {@code =}, e.g. {@code "50%"}) — reachable only
 *       by iterating {@link #entries()}.</li>
 * </ul>
 */
public class Settings {

    private final String name;
    private final Map<String, Setting> byKey = new HashMap<>();
    private final List<Setting> entries = new ArrayList<>();

    public Settings(String flagName) {
        this.name = flagName;
    }

    public String getName() {
        return name;
    }

    /**
     * Parse a settings string (everything after the leading flag name and
     * its first colon — e.g. {@code "min=20:max=50"} or {@code "50%"}).
     * Tokens of the form {@code k=v} become keyed entries; tokens without
     * {@code =} become positional entries.
     */
    public void load(String settingsString) {
        String[] tokens = settingsString.split(":");
        for (String token : tokens) {
            if (token.contains("=")) {
                String[] keyValue = token.split("=", 2);
                Setting setting = new Setting(keyValue[0], keyValue[1]);
                byKey.put(keyValue[0].toLowerCase(), setting);
                entries.add(setting);
            } else {
                entries.add(new Setting(token));
            }
        }
    }

    /** True if a {@code k=v} entry exists for {@code key} (case-insensitive). */
    public boolean contains(String key) {
        return byKey.containsKey(key.toLowerCase());
    }

    /** Look up the {@code k=v} entry for {@code key} (case-insensitive). */
    public Optional<Setting> get(String key) {
        return Optional.ofNullable(byKey.get(key.toLowerCase()));
    }

    /** Parse the value of {@code key=v} as an int. */
    public OptionalInt getInt(String key) {
        Setting s = byKey.get(key.toLowerCase());
        return s == null ? OptionalInt.empty() : s.getInt();
    }

    /** Parse the value of {@code key=v} as a double. */
    public OptionalDouble getDouble(String key) {
        Setting s = byKey.get(key.toLowerCase());
        return s == null ? OptionalDouble.empty() : s.getDouble();
    }

    /** Parse the value of {@code key=v} as a boolean (accepts only {@code "true"}/{@code "false"}). */
    public Optional<Boolean> getBoolean(String key) {
        Setting s = byKey.get(key.toLowerCase());
        return s == null ? Optional.empty() : s.getBoolean();
    }

    /** Raw string value of {@code key=v}. */
    public Optional<String> getString(String key) {
        Setting s = byKey.get(key.toLowerCase());
        return s == null ? Optional.empty() : Optional.of(s.asString());
    }

    /**
     * Unmodifiable view of every parsed entry in input order, including
     * positional tokens. Use this when the parsing logic doesn't fit a
     * single keyed lookup (positional values, suffix detection, multi-value
     * iteration).
     */
    public List<Setting> entries() {
        return Collections.unmodifiableList(entries);
    }
}

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

package de.Keyle.MyPet.api.config;

import de.Keyle.MyPet.MyPetApi;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central registry of every {@link ConfigKey} declared on a
 * {@link PetConfigKeys} nested class. Populated as a side effect of
 * each key's factory call ({@link ConfigKey#bool}, etc.).
 *
 * <p>Two storage layers:
 *
 * <ul>
 *   <li>{@link #KEYS_BY_PATH} — {@code (petType, key)} → {@link ConfigKey}
 *       lookup for {@link #lookup} and duplicate detection. Concurrent
 *       hashmap; safe for hot-path reads.</li>
 *   <li>{@link #ALL_KEYS} — iteration order preserved for YAML I/O. Copy-on-write
 *       so {@link #writeDefaults} and {@link #loadFromYaml} don't lock.</li>
 * </ul>
 *
 * <p>{@link #register} is called only by {@link ConfigKey} factories — the
 * package-private visibility prevents third-party code from constructing
 * raw {@code ConfigKey}s outside the {@link PetConfigKeys} declarations.
 */
public final class ConfigKeyRegistry {

    private ConfigKeyRegistry() {}

    private static final ConcurrentHashMap<String, ConfigKey<?>> KEYS_BY_PATH = new ConcurrentHashMap<>();
    private static final List<ConfigKey<?>> ALL_KEYS = new CopyOnWriteArrayList<>();

    /**
     * Adds {@code ck} to the registry. If a key with the same
     * {@code (petType, key)} pair is already registered, the existing
     * instance is kept and a warning is logged — duplicate registrations
     * usually indicate a third-party plugin declaring a key that
     * {@link PetConfigKeys} also owns.
     */
    static void register(ConfigKey<?> ck) {
        String mapKey = pathKey(ck.petType(), ck.key());
        ConfigKey<?> previous = KEYS_BY_PATH.putIfAbsent(mapKey, ck);
        if (previous != null) {
            MyPetApi.getLogger().warning(
                    "ConfigKeyRegistry: duplicate registration of " + ck.yamlPath()
                            + " — keeping the first instance, discarding the new one. "
                            + "If this came from a third-party plugin, reference "
                            + "PetConfigKeys." + ck.petType() + "." + ck.key()
                            + " instead of creating your own ConfigKey.");
            return;
        }
        ALL_KEYS.add(ck);
    }

    /**
     * Returns the {@link ConfigKey} for {@code (petType, key)}, or
     * {@code null} if none is registered. Used by {@link PetConfigLookup}
     * and by migrations that need to call {@link ConfigKey#update}.
     */
    public static ConfigKey<?> lookup(String petType, String key) {
        return KEYS_BY_PATH.get(pathKey(petType, key));
    }

    /**
     * Returns a snapshot of every registered key. Iteration order matches
     * registration order (so YAML output is stable across boots given the
     * same declarations).
     */
    public static List<ConfigKey<?>> all() {
        return new ArrayList<>(ALL_KEYS);
    }

    /**
     * Writes {@link ConfigKey#yamlDefault} for every registered key into
     * {@code config}. Caller must have already triggered
     * {@link PetConfigKeys#ensureLoaded()} so the registry is fully
     * populated.
     */
    public static void writeDefaults(Configuration config) {
        for (ConfigKey<?> ck : ALL_KEYS) {
            config.addDefault(ck.yamlPath(), ck.yamlDefault());
        }
    }

    /**
     * Reads each registered key's value from {@code config} and publishes
     * via {@link ConfigKey#loadFrom}. Safe to call concurrently with
     * {@link ConfigKey#get} on hot paths.
     */
    public static void loadFromYaml(ConfigurationSection config) {
        for (ConfigKey<?> ck : ALL_KEYS) {
            ck.loadFrom(config);
        }
    }

    private static String pathKey(String petType, String key) {
        return petType.toLowerCase(Locale.ROOT) + "::" + key.toLowerCase(Locale.ROOT);
    }
}

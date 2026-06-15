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
import de.Keyle.MyPet.api.entity.PetType;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central registry of every {@link ConfigKey} declared as a static field on
 * a pet class. Populated as a side effect of each key's factory call
 * ({@link ConfigKey#bool}, etc.) which fires during the pet class's
 * {@code <clinit>}.
 *
 * <p>{@link #ensurePetsLoaded()} force-initializes every registered pet
 * class via {@code Class.forName(true)} so the registry is fully populated
 * before {@link #writeDefaults} / {@link #loadFromYaml} iterate.
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
 * raw {@code ConfigKey}s outside the canonical pet-class declarations.
 */
public final class ConfigKeyRegistry {

    private ConfigKeyRegistry() {}

    private static final ConcurrentHashMap<String, ConfigKey<?>> KEYS_BY_PATH = new ConcurrentHashMap<>();
    private static final List<ConfigKey<?>> ALL_KEYS = new CopyOnWriteArrayList<>();
    private static volatile boolean petsLoaded = false;

    // Global keys (config.yml) are kept in a separate bucket from per-pet keys
    // (pet-config.yml) so writeDefaults/loadFromYaml and their global twins
    // each target the right file. Populated when MyPetGlobal's static fields
    // initialize (forced by ensureGlobalsLoaded).
    private static final ConcurrentHashMap<String, ConfigKey<?>> GLOBAL_BY_PATH = new ConcurrentHashMap<>();
    private static final List<ConfigKey<?>> GLOBAL_KEYS = new CopyOnWriteArrayList<>();
    private static volatile boolean globalsLoaded = false;

    /**
     * Force-initializes every registered pet class via {@code Class.forName(true)}
     * so each one's static {@link ConfigKey} field initializers fire and
     * register with this registry. Idempotent; runs once per JVM.
     *
     * <p>Each pet class is the canonical declaration site for its own
     * config keys — this method is what makes those declarations observable
     * to {@link #all}, {@link #writeDefaults}, and {@link #loadFromYaml}.
     */
    public static synchronized void ensurePetsLoaded() {
        if (petsLoaded) return;
        for (PetType petType : PetType.values()) {
            try {
                Class.forName(petType.getPetClass().getName(), true,
                        petType.getPetClass().getClassLoader());
            } catch (Throwable t) {
                MyPetApi.getLogger().warning(
                        "ConfigKeyRegistry: failed to load pet class for "
                                + petType.name() + ": " + t.getClass().getSimpleName()
                                + ": " + t.getMessage());
            }
        }
        petsLoaded = true;
    }

    /**
     * Adds {@code ck} to the registry. If a key with the same
     * {@code (petType, key)} pair is already registered, the existing
     * instance is kept and a warning is logged — duplicate registrations
     * usually indicate a third-party plugin declaring a key that the
     * matching pet class already owns.
     */
    static void register(ConfigKey<?> ck) {
        if (ck.isGlobal()) {
            ConfigKey<?> previous = GLOBAL_BY_PATH.putIfAbsent(ck.yamlPath(), ck);
            if (previous != null) {
                MyPetApi.getLogger().warning(
                        "ConfigKeyRegistry: duplicate registration of global key "
                                + ck.yamlPath() + " — keeping the first instance.");
                return;
            }
            GLOBAL_KEYS.add(ck);
            return;
        }
        String mapKey = pathKey(ck.petType(), ck.key());
        ConfigKey<?> previous = KEYS_BY_PATH.putIfAbsent(mapKey, ck);
        if (previous != null) {
            MyPetApi.getLogger().warning(
                    "ConfigKeyRegistry: duplicate registration of " + ck.yamlPath()
                            + " — keeping the first instance, discarding the new one. "
                            + "If this came from a third-party plugin, reference the "
                            + "existing static field on Pet" + ck.petType()
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
        ensurePetsLoaded();
        return KEYS_BY_PATH.get(pathKey(petType, key));
    }

    /**
     * Typed-bool convenience wrapper around {@link #lookup}. Returns the
     * current boolean value of the registered key, or {@code fallback} if
     * no key is registered for {@code (petType, key)} or the registered
     * key's value type isn't {@code Boolean}.
     *
     * <p>Used by the rideable-pet gating listeners — they look up
     * dynamically-registered flags ({@code RequireRideSkill},
     * {@code AllowNonOwnerPrimaryMount}, etc.) that have no static field
     * accessor on a specific Pet class.
     */
    public static boolean readBool(String petType, String key, boolean fallback) {
        ConfigKey<?> ck = lookup(petType, key);
        if (ck == null) return fallback;
        Object value = ck.get();
        return value instanceof Boolean b ? b : fallback;
    }

    /**
     * Returns a snapshot of every registered key. Iteration order matches
     * registration order (so YAML output is stable across boots given the
     * same declarations).
     */
    public static List<ConfigKey<?>> all() {
        ensurePetsLoaded();
        return new ArrayList<>(ALL_KEYS);
    }

    /**
     * Force-initializes {@code MyPetGlobal} (and, via its static block, every
     * nested section) so each global {@link ConfigKey} field initializer fires
     * and registers. Idempotent; runs once per JVM. The class is referenced by
     * name because it lives in the same {@code api} module but is not otherwise
     * a compile dependency of the registry.
     */
    public static synchronized void ensureGlobalsLoaded() {
        if (globalsLoaded) return;
        try {
            Class.forName("de.Keyle.MyPet.api.MyPetGlobal", true,
                    ConfigKeyRegistry.class.getClassLoader());
        } catch (Throwable t) {
            MyPetApi.getLogger().warning("ConfigKeyRegistry: failed to load MyPetGlobal: "
                    + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
        globalsLoaded = true;
    }

    /** Writes every global key's default into {@code config} (config.yml). */
    public static void writeGlobalDefaults(Configuration config) {
        ensureGlobalsLoaded();
        for (ConfigKey<?> ck : GLOBAL_KEYS) {
            config.addDefault(ck.yamlPath(), ck.yamlDefault());
        }
    }

    /** Reads every global key's value from {@code config} (config.yml). Hot-reloadable. */
    public static void loadGlobalsFromYaml(ConfigurationSection config) {
        ensureGlobalsLoaded();
        for (ConfigKey<?> ck : GLOBAL_KEYS) {
            ck.loadFrom(config);
        }
    }

    /** Writes {@link ConfigKey#yamlDefault} for every registered key into {@code config}. */
    public static void writeDefaults(Configuration config) {
        ensurePetsLoaded();
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
        ensurePetsLoaded();
        for (ConfigKey<?> ck : ALL_KEYS) {
            ck.loadFrom(config);
        }
    }

    private static String pathKey(String petType, String key) {
        return petType.toLowerCase(Locale.ROOT) + "::" + key.toLowerCase(Locale.ROOT);
    }
}

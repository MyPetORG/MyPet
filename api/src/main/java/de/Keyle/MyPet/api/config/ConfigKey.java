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

import de.Keyle.MyPet.api.util.ConfigItem;
import org.bukkit.configuration.ConfigurationSection;

import java.util.function.BiFunction;

/**
 * A per-pet config value, declared as a {@code public static final} field
 * on the matching {@code PetXxx} pet class — the pet class is the single
 * source of truth for its species' config. Each declaration self-registers
 * with {@link ConfigKeyRegistry} so plugin-side YAML loading and default
 * writing can iterate every key without further wiring.
 *
 * <p>Reads are {@code volatile} loads on the per-key cell — cheap enough for
 * hot paths (per-tick AI goals, per-event listeners). Hot reload publishes
 * new values via {@link #update}, so cached references automatically observe
 * the new value on their next {@link #get} call.
 *
 * <p>The YAML path is derived purely from {@link #petType} and {@link #key}:
 * {@code MyPet.Pets.<PetType>.<Key>}. The class lives in api (no plugin-side
 * dependencies), so third-party plugins can hold typed references directly.
 *
 * @param <T> the value type — {@link Boolean} or {@link ConfigItem} in practice
 */
public final class ConfigKey<T> {

    private final String petType;
    private final String key;
    private final String explicitPath;
    private final T defaultValue;
    private final Object yamlDefault;
    private final BiFunction<ConfigurationSection, String, T> reader;

    private volatile T value;

    private ConfigKey(String petType, String key, String explicitPath, T defaultValue, Object yamlDefault,
                      BiFunction<ConfigurationSection, String, T> reader) {
        this.petType = petType;
        this.key = key;
        this.explicitPath = explicitPath;
        this.defaultValue = defaultValue;
        this.yamlDefault = yamlDefault;
        this.reader = reader;
        this.value = defaultValue;
    }

    /** Current value — volatile read; safe to call from any thread. */
    public T get() {
        return value;
    }

    /** Compile-time default at the {@code T} type. Used by reload + reset paths. */
    public T defaultValue() {
        return defaultValue;
    }

    /**
     * Value written to YAML by {@link ConfigKeyRegistry#writeDefaults}. Equals
     * {@link #defaultValue} for primitive types; for {@link ConfigItem} keys
     * it's the source string (e.g. {@code "experience_bottle"}) since
     * {@code addDefault} can't serialize a {@code ConfigItem} directly.
     */
    public Object yamlDefault() {
        return yamlDefault;
    }

    /** Pet type name (e.g. {@code "Creeper"}, {@code "ZombieVillager"}). */
    public String petType() {
        return petType;
    }

    /** Bare key name (e.g. {@code "AllowLightningPower"}). Null for global keys. */
    public String key() {
        return key;
    }

    /**
     * Whether this is a global key (lives in {@code config.yml} at an explicit
     * path) rather than a per-pet key (lives in {@code pet-config.yml} under
     * {@code MyPet.Pets.<Type>.<Key>}).
     */
    public boolean isGlobal() {
        return explicitPath != null;
    }

    /**
     * Full YAML path. Per-pet keys derive {@code MyPet.Pets.<PetType>.<Key>};
     * global keys return their explicit, hand-authored path.
     */
    public String yamlPath() {
        return explicitPath != null ? explicitPath : "MyPet.Pets." + petType + "." + key;
    }

    /**
     * Reads this key's value from {@code config} and publishes it via
     * volatile write. Called by {@link ConfigKeyRegistry#loadFromYaml} on
     * plugin enable and {@code /petadmin reload}.
     */
    public void loadFrom(ConfigurationSection config) {
        this.value = reader.apply(config, yamlPath());
    }

    /**
     * Publishes a new value via volatile write. Intended for migrations that
     * need to update the in-memory cell after rewriting the YAML mid-boot
     * (after {@link #loadFrom} already ran with stale defaults).
     */
    public void update(T newValue) {
        this.value = newValue;
    }

    // =====================================================================
    // Factories — each call constructs a ConfigKey AND registers it with
    // ConfigKeyRegistry as a side effect.
    // =====================================================================

    /** Boolean key with explicit default. */
    public static ConfigKey<Boolean> bool(String petType, String key, boolean defaultValue) {
        ConfigKey<Boolean> ck = new ConfigKey<>(petType, key, null, defaultValue, defaultValue,
                (config, path) -> config.getBoolean(path, defaultValue));
        ConfigKeyRegistry.register(ck);
        return ck;
    }

    /**
     * {@code GrowUpItem} key for {@code PetBaby} types. The default material
     * name is passed explicitly (rather than read reflectively from a pet
     * class's {@code @DefaultInfo}) since this class lives in api with no
     * dependency on plugin-side pet types.
     *
     * @param petType         pet type name (e.g. {@code "Horse"})
     * @param defaultMaterial material name in lowercase (e.g. {@code "bread"})
     */
    public static ConfigKey<ConfigItem> growUpItem(String petType, String defaultMaterial) {
        String defaultString = defaultMaterial.toLowerCase();
        ConfigItem defaultItem = ConfigItem.createConfigItem(defaultString);
        ConfigKey<ConfigItem> ck = new ConfigKey<>(petType, "GrowUpItem", null, defaultItem, defaultString,
                (config, path) -> ConfigItem.createConfigItem(config.getString(path, defaultString)));
        ConfigKeyRegistry.register(ck);
        return ck;
    }

    // =====================================================================
    // Global factories — for the plugin-wide settings in config.yml that
    // were previously public static fields on the Configuration class. Unlike
    // per-pet keys, the YAML path is explicit (the paths are hand-authored and
    // irregular, e.g. "MyPet.Make-Pet-Invisible-When-Owner-Is-Invisible"), and
    // these register into ConfigKeyRegistry's separate global bucket so plugin
    // I/O targets config.yml rather than pet-config.yml. Declared as static
    // fields on MyPetGlobal.
    // =====================================================================

    /**
     * General-purpose global key. Most callers want one of the typed wrappers
     * below; use this directly only when a custom {@code reader} is needed
     * (e.g. post-read normalization or enum validation).
     *
     * @param path         full YAML path in {@code config.yml}
     * @param defaultValue compile-time default at type {@code T}
     * @param yamlDefault  value written to YAML by {@code writeGlobalDefaults}
     * @param reader       reads and converts the value from a config section
     */
    public static <T> ConfigKey<T> global(String path, T defaultValue, Object yamlDefault,
                                          BiFunction<ConfigurationSection, String, T> reader) {
        ConfigKey<T> ck = new ConfigKey<>(null, null, path, defaultValue, yamlDefault, reader);
        ConfigKeyRegistry.register(ck);
        return ck;
    }

    /** Global boolean key. */
    public static ConfigKey<Boolean> globalBool(String path, boolean defaultValue) {
        return global(path, defaultValue, defaultValue, (config, p) -> config.getBoolean(p, defaultValue));
    }

    /** Global int key. */
    public static ConfigKey<Integer> globalInt(String path, int defaultValue) {
        return global(path, defaultValue, defaultValue, (config, p) -> config.getInt(p, defaultValue));
    }

    /** Global long key. */
    public static ConfigKey<Long> globalLong(String path, long defaultValue) {
        return global(path, defaultValue, defaultValue, (config, p) -> config.getLong(p, defaultValue));
    }

    /** Global double key. */
    public static ConfigKey<Double> globalDouble(String path, double defaultValue) {
        return global(path, defaultValue, defaultValue, (config, p) -> config.getDouble(p, defaultValue));
    }

    /** Global string key. */
    public static ConfigKey<String> globalString(String path, String defaultValue) {
        return global(path, defaultValue, defaultValue, (config, p) -> config.getString(p, defaultValue));
    }

    /** Global string-set key — reads a YAML list into a {@link java.util.Set}. */
    public static ConfigKey<java.util.Set<String>> globalStringSet(String path) {
        return global(path, new java.util.HashSet<>(), new java.util.ArrayList<>(),
                (config, p) -> new java.util.HashSet<>(config.getStringList(p)));
    }

    /**
     * Global {@link ConfigItem} key. The YAML default is the source material
     * string (e.g. {@code "lead"}) since {@code addDefault} can't serialize a
     * {@code ConfigItem} directly — mirrors {@link #growUpItem}.
     */
    public static ConfigKey<ConfigItem> globalItem(String path, String defaultMaterial) {
        String defaultString = defaultMaterial.toLowerCase();
        ConfigItem defaultItem = ConfigItem.createConfigItem(defaultString);
        return global(path, defaultItem, defaultString,
                (config, p) -> ConfigItem.createConfigItem(config.getString(p, defaultString)));
    }
}

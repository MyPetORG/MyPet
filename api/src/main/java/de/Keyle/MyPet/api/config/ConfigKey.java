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
    private final T defaultValue;
    private final Object yamlDefault;
    private final BiFunction<ConfigurationSection, String, T> reader;

    private volatile T value;

    private ConfigKey(String petType, String key, T defaultValue, Object yamlDefault,
                      BiFunction<ConfigurationSection, String, T> reader) {
        this.petType = petType;
        this.key = key;
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

    /** Bare key name (e.g. {@code "AllowLightningPower"}). */
    public String key() {
        return key;
    }

    /** Full YAML path: {@code MyPet.Pets.<PetType>.<Key>}. */
    public String yamlPath() {
        return "MyPet.Pets." + petType + "." + key;
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
        ConfigKey<Boolean> ck = new ConfigKey<>(petType, key, defaultValue, defaultValue,
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
        ConfigKey<ConfigItem> ck = new ConfigKey<>(petType, "GrowUpItem", defaultItem, defaultString,
                (config, path) -> ConfigItem.createConfigItem(config.getString(path, defaultString)));
        ConfigKeyRegistry.register(ck);
        return ck;
    }
}

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

import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.util.ConfigItem;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves per-pet config values from a runtime {@code Class<? extends Pet>}
 * reference — the form marker interfaces ({@code PetFlyingEntity.canFly},
 * {@code PetSwimmingEntity.canSwim}, etc.) need. Callers with a compile-time
 * dependency on plugin pet classes should prefer the direct field reference
 * (e.g. {@code PetCreeper.ALLOW_LIGHTNING_POWER.get()}) which gives typed
 * access and skips the class-name parsing.
 *
 * <p>This class converts the pet class to its pet-type name (stripping the
 * {@code "Pet"} prefix from the simple name) and delegates to
 * {@link ConfigKeyRegistry}, which transitively loads every pet class on
 * first call so all keys are registered before any read.
 */
public final class PetConfigLookup {

    private PetConfigLookup() {}

    /** Memoized {@code (petClass, key)} → {@link ConfigKey} so hot-path reads skip the name derivation. */
    private static final ClassValue<ConcurrentHashMap<String, ConfigKey<?>>> KEY_CACHE = new ClassValue<>() {
        @Override
        protected ConcurrentHashMap<String, ConfigKey<?>> computeValue(Class<?> type) {
            return new ConcurrentHashMap<>();
        }
    };

    /**
     * Returns the boolean value for {@code (petClass, key)} from the registry,
     * or {@code fallback} if no such key is registered (e.g. a third-party
     * pet that hasn't declared this flag).
     */
    public static boolean boolValue(Class<? extends Pet> petClass, String key, boolean fallback) {
        ConfigKey<?> ck = resolve(petClass, key);
        if (ck != null && ck.get() instanceof Boolean b) {
            return b;
        }
        return fallback;
    }

    /**
     * Returns the {@link ConfigItem} for {@code (petClass, key)}, or
     * {@code null} if no such key is registered.
     */
    public static ConfigItem configItemValue(Class<? extends Pet> petClass, String key) {
        ConfigKey<?> ck = resolve(petClass, key);
        if (ck != null && ck.get() instanceof ConfigItem ci) {
            return ci;
        }
        return null;
    }

    private static ConfigKey<?> resolve(Class<? extends Pet> petClass, String key) {
        ConcurrentHashMap<String, ConfigKey<?>> cache = KEY_CACHE.get(petClass);
        ConfigKey<?> ck = cache.get(key);
        if (ck != null) return ck;
        ck = ConfigKeyRegistry.lookup(petTypeName(petClass), key);
        if (ck != null) {
            // Misses stay uncached: third-party keys may register after first lookup.
            cache.putIfAbsent(key, ck);
        }
        return ck;
    }

    /** Pet type name = pet class's simple name with the {@code "Pet"} prefix stripped. */
    private static String petTypeName(Class<? extends Pet> petClass) {
        String simple = petClass.getSimpleName();
        return simple.startsWith("Pet") ? simple.substring(3) : simple;
    }
}

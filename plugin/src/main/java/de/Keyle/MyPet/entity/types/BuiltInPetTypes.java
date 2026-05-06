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

package de.Keyle.MyPet.entity.types;

import de.Keyle.MyPet.api.entity.Pet;
import de.Keyle.MyPet.api.entity.PetType;
import org.bukkit.entity.EntityType;

/**
 * Registers MyPet's bundled pet-type implementations with {@link PetType}.
 *
 * <p>The registration set is discovered by walking Bukkit's {@link EntityType} enum and
 * resolving each entry to a {@code de.Keyle.MyPet.entity.types.Pet<CamelName>} class via
 * {@link Class#forName}. The lazy resolution is load-bearing: pet types whose Bukkit
 * counterpart does not exist on the running server (e.g. {@code CopperGolem} on a Paper
 * version that predates it) are silently skipped without ever triggering JVM verification
 * of their {@code Pet*} class — which would fail because those classes import Paper types
 * that do not exist on older versions.</p>
 *
 * <p>Invoked once during {@code MyPetPlugin.onLoad} before any code that resolves pet
 * types. Idempotent: types already registered (e.g., by a previous {@code onLoad} during
 * a soft reload) are skipped.</p>
 */
public final class BuiltInPetTypes {

    private static final String IMPL_TYPES_PACKAGE = "de.Keyle.MyPet.entity.types.Pet";

    private BuiltInPetTypes() {
    }

    public static void register() {
        for (EntityType entityType : EntityType.values()) {
            String camelName = snakeToCamel(entityType.name());
            if (PetType.byNameOrNull(camelName) != null) {
                continue;
            }
            String className = IMPL_TYPES_PACKAGE + camelName;
            try {
                Class<?> clazz = Class.forName(className);
                if (Pet.class.isAssignableFrom(clazz)) {
                    @SuppressWarnings("unchecked")
                    Class<? extends Pet> petClass = (Class<? extends Pet>) clazz;
                    PetType.register(camelName, petClass);
                }
            } catch (ClassNotFoundException ignored) {
            }
        }
    }

    private static String snakeToCamel(String snake) {
        StringBuilder sb = new StringBuilder();
        boolean capitalize = true;
        for (int i = 0; i < snake.length(); i++) {
            char c = snake.charAt(i);
            if (c == '_') {
                capitalize = true;
            } else {
                sb.append(capitalize ? Character.toUpperCase(c) : Character.toLowerCase(c));
                capitalize = false;
            }
        }
        return sb.toString();
    }
}

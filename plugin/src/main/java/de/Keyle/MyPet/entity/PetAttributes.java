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

package de.Keyle.MyPet.entity;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;

/**
 * Version-portable resolution of vanilla attributes whose Bukkit constant names
 * changed in 1.21.3 (Mojang dropped the {@code generic.} prefix from attribute
 * resource keys). Resolves through {@link Registry#ATTRIBUTE} so the same field
 * works on 1.20.5 – 1.21.2 ({@code minecraft:generic.max_health}) and on
 * 1.21.3+ ({@code minecraft:max_health}).
 *
 * <p>Direct references to {@code Attribute.GENERIC_MAX_HEALTH} cannot be used
 * because that constant has been removed from the Paper API the project compiles
 * against; direct references to {@code Attribute.MAX_HEALTH} would throw
 * {@code NoSuchFieldError} on pre-1.21.3 servers.
 *
 * <p><b>Removal:</b> Once {@code plugin.yml}'s {@code api-version} is raised to
 * {@code 1.21.3} or higher, delete this class and replace every
 * {@code PetAttributes.X} call site with {@code Attribute.X}.
 */
public final class PetAttributes {

    public static final Attribute MAX_HEALTH = resolve("max_health", "generic.max_health");
    public static final Attribute MOVEMENT_SPEED = resolve("movement_speed", "generic.movement_speed");
    public static final Attribute KNOCKBACK_RESISTANCE = resolve("knockback_resistance", "generic.knockback_resistance");
    public static final Attribute FLYING_SPEED = resolve("flying_speed", "generic.flying_speed");

    private PetAttributes() {
    }

    private static Attribute resolve(String modernKey, String legacyKey) {
        Attribute attribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(modernKey));
        if (attribute == null) {
            attribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(legacyKey));
        }
        if (attribute == null) {
            throw new IllegalStateException(
                    "Could not resolve attribute '" + modernKey + "' (legacy '" + legacyKey + "') from server registry");
        }
        return attribute;
    }
}

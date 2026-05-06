/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2025 Keyle
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

package de.Keyle.MyPet.api.entity;

import de.Keyle.MyPet.api.exceptions.PetTypeNotFoundException;
import lombok.Getter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;

import java.util.*;

/**
 * Registry of all available pet types. Built-in types are populated by
 * {@code MyPetPlugin.onLoad} via {@code BuiltInPetTypes.register()} (plugin module);
 * third-party plugins register custom types via {@link #register(String, Class)}.
 * <p>
 * Each impl class carries its own {@code @ShopInfo} / {@code @DefaultInfo} annotations
 * and {@code implements} the relevant marker interfaces ({@link MyPetBaby},
 * {@link MyPetFlyingEntity}, etc.) directly.
 */
public final class PetType {

    private static final Map<String, PetType> BY_NAME = new LinkedHashMap<>();
    private static final Map<String, PetType> BY_BUKKIT_NAME = new LinkedHashMap<>();

    private final String name;
    @Getter
    private final String bukkitName;
    private final Class<? extends MyPet> mypetClass;
    @Getter
    private final Class<? extends Mob> bukkitEntityClass;

    private PetType(String name, String bukkitName, Class<? extends MyPet> mypetClass) {
        this.name = name;
        this.bukkitName = bukkitName;
        this.mypetClass = mypetClass;
        this.bukkitEntityClass = resolveBukkitEntityClass(bukkitName);
    }

    private static Class<? extends Mob> resolveBukkitEntityClass(String bukkitName) {
        try {
            EntityType bukkitType = EntityType.valueOf(bukkitName);
            Class<? extends Entity> cls = bukkitType.getEntityClass();
            if (cls != null && Mob.class.isAssignableFrom(cls)) {
                return cls.asSubclass(Mob.class);
            }
        } catch (IllegalArgumentException ignored) {
        }
        return null;
    }

    /**
     * Registers a custom pet type. Use this for third-party or ModelEngine-backed entities.
     *
     * @param name     CamelCase name (e.g. "MyCustomMob")
     * @param petClass interface extending MyPet
     * @return the registered PetType
     * @throws IllegalArgumentException if a type with this name is already registered
     */
    public static PetType register(String name, Class<? extends MyPet> petClass) {
        String key = name.toUpperCase();
        if (BY_NAME.containsKey(key)) {
            throw new IllegalArgumentException("PetType '" + name + "' is already registered");
        }
        String bukkitName = camelToSnake(name).toUpperCase();
        PetType type = new PetType(name, bukkitName, petClass);
        BY_NAME.put(key, type);
        BY_BUKKIT_NAME.put(bukkitName, type);
        return type;
    }

    /**
     * Removes a previously registered pet type. Third-party plugins should call this from
     * their {@code onDisable} to release the {@code Class<? extends MyPet>} reference and
     * avoid leaking their plugin classloader across reloads. No-op if the name is unknown.
     */
    public static void unregister(String name) {
        PetType type = BY_NAME.remove(name.toUpperCase());
        if (type != null) {
            BY_BUKKIT_NAME.remove(type.bukkitName);
        }
    }

    private static String camelToSnake(String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append('_');
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    /** Returns a mutable copy of all registered types in insertion order. */
    public static List<PetType> all() {
        return new ArrayList<>(BY_NAME.values());
    }

    /** Returns an unmodifiable view of all registered types. */
    public static Collection<PetType> values() {
        return Collections.unmodifiableCollection(BY_NAME.values());
    }

    /**
     * Looks up a type by its CamelCase name (case-insensitive).
     *
     * @throws PetTypeNotFoundException if no type is registered
     */
    public static PetType valueOf(String name) {
        return byName(name);
    }

    /**
     * Looks up a type by its Bukkit {@link EntityType} enum name
     * (UPPER_SNAKE_CASE, case-insensitive).
     *
     * @throws PetTypeNotFoundException if no type is registered
     */
    public static PetType byEntityTypeName(String name) {
        PetType type = BY_BUKKIT_NAME.get(name.toUpperCase());
        if (type != null) {
            return type;
        }
        throw new PetTypeNotFoundException(name);
    }

    /**
     * Looks up a type by its CamelCase name (case-insensitive).
     *
     * @throws PetTypeNotFoundException if no type is registered
     */
    public static PetType byName(String name) {
        PetType type = BY_NAME.get(name.toUpperCase());
        if (type != null) {
            return type;
        }
        throw new PetTypeNotFoundException(name);
    }

    /**
     * Looks up a type by name, returning {@code null} instead of
     * throwing if the type is not registered.
     */
    public static PetType byNameOrNull(String name) {
        return BY_NAME.get(name.toUpperCase());
    }

    /** CamelCase display name (e.g. {@code "ZombieVillager"}). */
    public String name() {
        return name;
    }

    /** Lowercase type ID used for persistence keys (e.g. {@code "zombie_villager"}). */
    public String getTypeID() {
        return bukkitName.toLowerCase();
    }

    /** Returns the pet implementation class for this type. */
    public Class<? extends MyPet> getMyPetClass() {
        return mypetClass;
    }

    /**
     * Type-level capability check: does this pet type's interface declare
     * {@link MyPetFlyingEntity}? The per-pet {@code CanFly} preference is
     * checked separately at the pet *instance* level (see
     * {@link MyPetFlyingEntity#canFly()}) — callers that need the combined
     * capability and preference answer should use the instance check directly.
     */
    public boolean isFlyingPet() {
        return MyPetFlyingEntity.class.isAssignableFrom(mypetClass);
    }

    /** Type-level check: does this type implement {@link MyPetSwimmingEntity}? */
    public boolean isSwimmingPet() {
        return MyPetSwimmingEntity.class.isAssignableFrom(mypetClass);
    }

    /** Whether this pet type should float on lava (Strider). */
    public boolean floatsInLava() {
        return false;
    }

    /** Whether this type needs custom float logic beyond standard swim. */
    public boolean specialFloat() {
        return false;
    }

    /**
     * Returns {@code true} if the current Minecraft version supports
     * this pet type. Overridden for types added in later MC versions.
     */
    public boolean checkMinecraftVersion() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PetType other)) return false;
        return name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}

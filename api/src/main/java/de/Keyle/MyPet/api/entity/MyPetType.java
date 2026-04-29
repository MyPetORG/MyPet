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

import de.Keyle.MyPet.api.exceptions.MyPetTypeNotFoundException;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;

import java.util.*;

/**
 * Registry of all available pet types. Types are auto-discovered at startup
 * by scanning Bukkit's EntityType enum for matching My* implementation classes
 * in {@code de.Keyle.MyPet.entity.types} (the plugin module). Each impl class
 * carries its own {@code @ShopInfo} / {@code @DefaultInfo} annotations and
 * {@code implements} the relevant marker interfaces ({@link MyPetBaby},
 * {@link MyPetFlyingEntity}, etc.) directly.
 * <p>
 * Third-party plugins can register custom pet types via {@link #register(String, Class)}.
 */
public final class MyPetType {

    private static final String IMPL_TYPES_PACKAGE = "de.Keyle.MyPet.entity.types.My";
    private static final Map<String, MyPetType> BY_NAME = new LinkedHashMap<>();
    private static final Map<String, MyPetType> BY_BUKKIT_NAME = new LinkedHashMap<>();

    static {
        for (EntityType entityType : EntityType.values()) {
            String camelName = snakeToCamel(entityType.name());
            String className = IMPL_TYPES_PACKAGE + camelName;
            try {
                Class<?> clazz = Class.forName(className);
                if (MyPet.class.isAssignableFrom(clazz)) {
                    @SuppressWarnings("unchecked")
                    Class<? extends MyPet> petClass = (Class<? extends MyPet>) clazz;
                    MyPetType type = new MyPetType(camelName, entityType.name(), petClass);
                    BY_NAME.put(camelName.toUpperCase(), type);
                    BY_BUKKIT_NAME.put(entityType.name(), type);
                }
            } catch (ClassNotFoundException ignored) {
            }
        }
    }

    private final String name;
    private final String bukkitName;
    private final Class<? extends MyPet> mypetClass;
    private final Class<? extends Mob> bukkitEntityClass;

    private MyPetType(String name, String bukkitName, Class<? extends MyPet> mypetClass) {
        this.name = name;
        this.bukkitName = bukkitName;
        this.mypetClass = mypetClass;
        this.bukkitEntityClass = resolveBukkitEntityClass(bukkitName);
    }

    @SuppressWarnings("unchecked")
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
     * @return the registered MyPetType
     */
    public static MyPetType register(String name, Class<? extends MyPet> petClass) {
        String bukkitName = camelToSnake(name).toUpperCase();
        MyPetType type = new MyPetType(name, bukkitName, petClass);
        BY_NAME.put(name.toUpperCase(), type);
        BY_BUKKIT_NAME.put(bukkitName, type);
        return type;
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

    public static List<MyPetType> all() {
        return new ArrayList<>(BY_NAME.values());
    }

    public static Collection<MyPetType> values() {
        return Collections.unmodifiableCollection(BY_NAME.values());
    }

    public static MyPetType valueOf(String name) {
        return byName(name);
    }

    public static MyPetType byEntityTypeName(String name) {
        return byEntityTypeName(name, true);
    }

    public static MyPetType byEntityTypeName(String name, boolean versionCheck) {
        MyPetType type = BY_BUKKIT_NAME.get(name.toUpperCase());
        if (type != null) {
            return type;
        }
        throw new MyPetTypeNotFoundException(name);
    }

    public static MyPetType byName(String name) {
        return byName(name, true);
    }

    public static MyPetType byName(String name, boolean versionCheck) {
        MyPetType type = BY_NAME.get(name.toUpperCase());
        if (type != null) {
            return type;
        }
        throw new MyPetTypeNotFoundException(name);
    }

    public static MyPetType byNameOrNull(String name) {
        return BY_NAME.get(name.toUpperCase());
    }

    public String name() {
        return name;
    }

    public String getBukkitName() {
        return bukkitName;
    }

    public String getTypeID() {
        return bukkitName.toLowerCase();
    }

    public Class<? extends MyPet> getMyPetClass() {
        return mypetClass;
    }

    public Class<? extends Mob> getBukkitEntityClass() {
        return bukkitEntityClass;
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

    public boolean isAquaticPet() {
        return MyPetAquaticEntity.class.isAssignableFrom(mypetClass);
    }

    public boolean floatsInLava() {
        return false;
    }

    public boolean specialFloat() {
        return false;
    }

    public boolean checkMinecraftVersion() {
        return true;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MyPetType other)) return false;
        return name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}

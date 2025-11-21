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

package de.Keyle.MyPet.api.util;

import lombok.NonNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class ReflectionUtil {
    private static MethodHandles.Lookup METHODHANDLE_LOOKUPER = MethodHandles.lookup();
    private static Field MODIFIERS_FIELD;
    private static Object THE_UNSAFE;
    private static MethodHandle PUT_OBJECT;
    private static MethodHandle STATIC_FIELD_OFFSET;

    static {
        MODIFIERS_FIELD = getField(Field.class, "modifiers");
    }

    /**
     * Loads a class by its fully qualified name using Class.forName().
     *
     * @param name the fully qualified class name (e.g., "java.util.ArrayList")
     * @return the Class object representing the loaded class
     * @throws RuntimeException if the class cannot be found or loaded
     */
    @SuppressWarnings("rawtypes")
    public static Class getClass(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable e) {
            throw new RuntimeException("failed to load a class", e);
        }
    }

    /**
     * Retrieves a declared method from a class and makes it accessible.
     * This method can access private, protected, and package-private methods.
     *
     * @param clazz the class containing the method
     * @param method the name of the method
     * @param parameterTypes the parameter types of the method (optional, for overloaded methods)
     * @return the Method object, or null if the method is not found
     */
    public static Method getMethod(Class<?> clazz, String method, Class<?>... parameterTypes) {
        try {
            Method m = clazz.getDeclaredMethod(method, parameterTypes);
            m.setAccessible(true);
            return m;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Retrieves a declared field from a class and makes it accessible.
     * This method can access private, protected, and package-private fields.
     *
     * @param clazz the class containing the field
     * @param field the name of the field
     * @return the Field object, or null if the field is not found
     */
    public static Field getField(Class<?> clazz, String field) {
        try {
            Field f = clazz.getDeclaredField(field);
            f.setAccessible(true);
            return f;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Retrieves the value of a field from a target object by field name.
     * The field is accessed reflectively and can be private, protected, or package-private.
     *
     * @param clazz the class containing the field
     * @param target the object instance to get the field value from (null for static fields)
     * @param field the name of the field
     * @return the value of the field, or null if the field is not found or cannot be accessed
     */
    public static Object getFieldValue(Class<?> clazz, Object target, String field) {
        try {
            Field f = clazz.getDeclaredField(field);
            f.setAccessible(true);
            return f.get(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Retrieves the value of a field from a target object using a Field object.
     *
     * @param field the Field object representing the field to access (must not be null)
     * @param target the object instance to get the field value from (null for static fields)
     * @return the value of the field, or null if the field cannot be accessed
     */
    public static Object getFieldValue(@NonNull Field field, Object target) {
        try {
            return field.get(target);
        } catch (Throwable ignored) {
        }
        return null;
    }

    /**
     * Sets the value of a field on a target object using a Field object.
     *
     * @param field the Field object representing the field to modify (must not be null)
     * @param target the object instance to set the field value on (null for static fields)
     * @param value the new value to assign to the field
     * @return true if the field was successfully set, false otherwise
     */
    public static boolean setFieldValue(@NonNull Field field, Object target, Object value) {
        try {
            field.set(target, value);
            return true;
        } catch (Throwable e) {
            return false;
        }

    }

    /**
     * Sets the value of a field on a target object by field name.
     * The field is accessed reflectively and can be private, protected, or package-private.
     *
     * @param fieldName the name of the field to modify
     * @param target the object instance to set the field value on (null for static fields)
     * @param value the new value to assign to the field
     * @return true if the field was successfully set, false if the field is not found or cannot be set
     */
    public static boolean setFieldValue(String fieldName, Object target, Object value) {
        try {
            Field field = getField(target.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                field.set(target, value);
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /**
     * Sets the value of a final field on a target object by field name.
     * This method removes the final modifier before setting the value, allowing modification
     * of fields declared as final. Use with caution as this breaks Java's immutability guarantees.
     *
     * @param fieldName the name of the final field to modify
     * @param target the object instance to set the field value on (null for static fields)
     * @param value the new value to assign to the field
     * @return true if the field was successfully set, false if the field is not found or cannot be set
     */
    public static boolean setFinalFieldValue(String fieldName, Object target, Object value) {
        try {
            Field field = getField(target.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);

                Field modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

                field.set(target, value);
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /**
     * Sets the value of a final field on a target object using a Field object.
     * This method removes the final modifier before setting the value on Java versions before 12.
     * On Java 12+, the modifiers field is not accessible, but final field modification still works
     * through the Unsafe API internally.
     *
     * @param field the Field object representing the final field to modify (must not be null)
     * @param target the object instance to set the field value on (null for static fields)
     * @param value the new value to assign to the field
     * @return true if the field was successfully set, false if the field cannot be set
     */
    public static boolean setFinalFieldValue(@NonNull Field field, Object target, Object value) {
        try {
            field.setAccessible(true);

            if(Integer.parseInt(System.getProperty("java.version").split("\\.")[0]) < 12) { //Java-Version-Check
                Field modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            }

            field.set(target, value);
            return true;
        } catch (Throwable ignored) {
        }
        return false;
    }

    /**
     * Creates a MethodHandle that can set the value of a static final field.
     * This is necessary for modifying static final fields, which cannot be changed through
     * normal reflection on newer Java versions. The method uses Unsafe API when the modifiers
     * field is not accessible (Java 12+).
     *
     * @param className the class containing the static final field
     * @param fieldName the name of the static final field
     * @return a MethodHandle for setting the field, or null if the field is not found or cannot be accessed
     */
    public static MethodHandle createStaticFinalSetter(Class<?> className, String fieldName) {
        Field field = getField(className, fieldName);
        if (field == null) {
            return null;
        }

        if (MODIFIERS_FIELD == null) {
            if (THE_UNSAFE == null) {
                try {
                    THE_UNSAFE = getField(Class.forName("sun.misc.Unsafe"), "theUnsafe").get(null);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
                STATIC_FIELD_OFFSET = getMethodHandle(THE_UNSAFE.getClass(), "staticFieldOffset", Field.class).bindTo(THE_UNSAFE);
                PUT_OBJECT = getMethodHandle(THE_UNSAFE.getClass(), "putObject",  Object.class, long.class, Object.class).bindTo(THE_UNSAFE);
            }

            try {
                long offset = (long) STATIC_FIELD_OFFSET.invoke(field);

                return MethodHandles.insertArguments(PUT_OBJECT, 0, className, offset);
            } catch (Throwable t) {
                t.printStackTrace();
                return null;
            }
        }

        try {
            MODIFIERS_FIELD.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            return METHODHANDLE_LOOKUPER.unreflectSetter(field);
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Creates a MethodHandle for a method, allowing for efficient repeated invocation.
     * MethodHandles provide better performance than standard reflection for repeated calls.
     *
     * @param className the class containing the method
     * @param method the name of the method
     * @param parameters the parameter types of the method (optional, for overloaded methods)
     * @return a MethodHandle for the method, or null if the method is not found or className is null
     */
    public static MethodHandle getMethodHandle(Class<?> className, String method, Class<?>... parameters) {
        if (className != null) {
            try {
                return METHODHANDLE_LOOKUPER.unreflect(getMethod(className, method, parameters));
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /**
     * Checks if a class is a subclass of or equal to another class by recursively
     * checking the superclass hierarchy.
     *
     * @param clazz the class to check
     * @param superClass the potential superclass to check against
     * @return true if clazz is equal to or extends superClass, false otherwise
     */
    public static boolean isTypeOf(Class<?> clazz, Class<?> superClass) {
        if (!clazz.equals(superClass)) {
            clazz = clazz.getSuperclass();
            return !clazz.equals(Object.class) && isTypeOf(clazz, superClass);
        }
        return true;
    }
}

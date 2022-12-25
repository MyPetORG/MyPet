/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2020 Keyle
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

    @SuppressWarnings("rawtypes")
    public static Class getClass(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable ignored) {
            throw new RuntimeException("failed to load a class", ignored);
        }
    }

    public static Method getMethod(Class<?> clazz, String method, Class<?>... parameterTypes) {
        try {
            Method m = clazz.getDeclaredMethod(method, parameterTypes);
            m.setAccessible(true);
            return m;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Field getField(Class<?> clazz, String field) {
        try {
            Field f = clazz.getDeclaredField(field);
            f.setAccessible(true);
            return f;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Object getFieldValue(Class<?> clazz, Object target, String field) {
        try {
            Field f = clazz.getDeclaredField(field);
            f.setAccessible(true);
            return f.get(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Object getFieldValue(@NonNull Field field, Object target) {
        try {
            return field.get(target);
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static boolean setFieldValue(@NonNull Field field, Object target, Object value) {
        try {
            field.set(target, value);
            return true;
        } catch (Throwable e) {
            return false;
        }

    }

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
        } catch (Exception e) {
        }
        return null;
    }

    public static MethodHandle getMethodHandle(Class<?> className, String method, Class<?>... parameters) {
        if (className != null) {
            try {
                return METHODHANDLE_LOOKUPER.unreflect(getMethod(className, method, parameters));
            } catch (Exception e) {
            }
        }
        return null;
    }

    public static boolean isTypeOf(Class<?> clazz, Class<?> superClass) {
        if (!clazz.equals(superClass)) {
            clazz = clazz.getSuperclass();
            return !clazz.equals(Object.class) && isTypeOf(clazz, superClass);
        }
        return true;
    }
}

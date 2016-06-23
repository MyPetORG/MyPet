/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2016 Keyle
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectionUtil {
    public static Method getMethod(Class<?> clazz, String method, Class<?>... parameterTypes) {
        try {
            Method m = clazz.getDeclaredMethod(method, parameterTypes);
            m.setAccessible(true);
            return m;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static Field getField(Class<?> clazz, String field) {
        try {
            Field f = clazz.getDeclaredField(field);
            f.setAccessible(true);
            return f;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static Object getFieldValue(Class<?> clazz, Object object, String field) {
        try {
            Field f = clazz.getDeclaredField(field);
            f.setAccessible(true);
            return f.get(object);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static boolean setFieldValue(Field field, Object object, Object value) {
        try {
            field.set(object, value);
            return true;
        } catch (IllegalAccessException e) {
            return false;
        }

    }

    public static boolean setFieldValue(String fieldName, Object object, Object value) {
        try {
            Field field = getField(object.getClass(), fieldName);
            if (field != null) {
                field.set(object, value);
                return true;
            }
        } catch (IllegalAccessException ignored) {
        }
        return false;
    }
}

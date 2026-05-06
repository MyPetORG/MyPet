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

package de.Keyle.MyPet.api;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;

public class Util {

    /**
     * A restricted MiniMessage instance that only resolves safe visual tags.
     * Use this for user-controlled content (pet names, etc.) to prevent
     * injection of click/hover/insertion events via MiniMessage tags.
     */
    public static final MiniMessage SANITIZED_MINIMESSAGE = MiniMessage.builder()
            .tags(TagResolver.resolver(
                    StandardTags.color(),
                    StandardTags.decorations(),
                    StandardTags.reset(),
                    StandardTags.gradient(),
                    StandardTags.rainbow()
            ))
            .build();

    static final Random rng = new Random();

    /**
     * Returns the shared {@link Random} instance used throughout the plugin.
     *
     * @return the shared random number generator
     */
    public static Random getRandom() {
        return rng;
    }

    /**
     * Checks whether the given string can be parsed as an {@code int}.
     *
     * @param number the string to test
     * @return {@code true} if the string is a valid integer, {@code false} otherwise
     */
    public static boolean isInt(String number) {
        try {
            Integer.parseInt(number);
            return true;
        } catch (NumberFormatException nFE) {
            return false;
        }
    }

    /**
     * Fetches the content of a URL as a string using a default timeout of 2000ms.
     *
     * @param address the URL to fetch
     * @return the response body as a string
     * @throws IOException if the request fails or is interrupted
     * @see #readUrlContent(String, int)
     */
    public static String readUrlContent(String address) throws IOException {
        return readUrlContent(address, 2000);
    }

    /**
     * Fetches the content of a URL as a string with a configurable timeout.
     * The timeout applies to both the connection and the request as a whole.
     *
     * @param address the URL to fetch
     * @param timeout the timeout in milliseconds for both connect and read
     * @return the response body as a string
     * @throws IOException if the request fails or is interrupted
     */
    public static String readUrlContent(String address, int timeout) throws IOException {
        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeout))
                .build()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(address))
                    .timeout(Duration.ofMillis(timeout))
                    .GET()
                    .build();
            return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("HTTP request interrupted", e);
        }
    }

    /**
     * Converts a positive integer to its Roman numeral representation.
     * Used for displaying skill levels (e.g. level 4 as "IV").
     *
     * @param src the positive integer to convert
     * @return the Roman numeral string, or an empty string if {@code src} is 0
     */
    public static String decimal2roman(int src) {
        char[] digits = {'I', 'V', 'X', 'L', 'C', 'D', 'M'};
        StringBuilder thousands = new StringBuilder();
        StringBuilder result = new StringBuilder();
        int rang, digit, i;

        for (i = src / 1000; i > 0; i--) {
            thousands.append("M");
        }
        src %= 1000;

        rang = 0;
        while (src > 0) {
            digit = src % 10;
            src /= 10;
            switch (digit) {
                case 1:
                    result.insert(0, "" + digits[rang]);
                    break;
                case 2:
                    result.insert(0, "" + digits[rang] + digits[rang]);
                    break;
                case 3:
                    result.insert(0, "" + digits[rang] + digits[rang] + digits[rang]);
                    break;
                case 4:
                    result.insert(0, "" + digits[rang] + digits[rang + 1]);
                    break;
                case 5:
                    result.insert(0, "" + digits[rang + 1]);
                    break;
                case 6:
                    result.insert(0, "" + digits[rang + 1] + digits[rang]);
                    break;
                case 7:
                    result.insert(0, "" + digits[rang + 1] + digits[rang] + digits[rang]);
                    break;
                case 8:
                    result.insert(0, "" + digits[rang + 1] + digits[rang] + digits[rang] + digits[rang]);
                    break;
                case 9:
                    result.insert(0, "" + digits[rang] + digits[rang + 2]);
                    break;
            }
            rang += 2;
        }
        return thousands.toString() + result;
    }

    /**
     * Clamps a {@code double} value to the specified range.
     *
     * @param val the value to clamp
     * @param min the minimum allowed value
     * @param max the maximum allowed value
     * @return {@code min} if {@code val < min}, {@code max} if {@code val > max}, otherwise {@code val}
     */
    public static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    /**
     * Clamps a {@code float} value to the specified range.
     *
     * @param val the value to clamp
     * @param min the minimum allowed value
     * @param max the maximum allowed value
     * @return {@code min} if {@code val < min}, {@code max} if {@code val > max}, otherwise {@code val}
     */
    public static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    /**
     * Clamps an {@code int} value to the specified range.
     *
     * @param val the value to clamp
     * @param min the minimum allowed value
     * @param max the maximum allowed value
     * @return {@code min} if {@code val < min}, {@code max} if {@code val > max}, otherwise {@code val}
     */
    public static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    /**
     * Recursively collects all superclasses and interfaces of {@code clazz} that are
     * assignable to {@code type}, excluding {@code type} itself and {@link Object}.
     *
     * @param <T>    the base type to filter against
     * @param clazz  the class whose hierarchy to walk
     * @param type   the target type — only classes assignable to this are collected
     * @param result the set to populate with matching parent classes
     */
    @SuppressWarnings("unchecked")
    public static <T> void getClassParents(Class<?> clazz, Class<T> type, Set<Class<? extends T>> result) {
        if (type != null && clazz != null && result != null && clazz != type) {
            if (clazz == Object.class) {
                return;
            }
            if (type.isAssignableFrom(clazz)) {
                result.add((Class<? extends T>) clazz);
            }
            getClassParents(clazz.getSuperclass(), type, result);
            for (Class<?> c : clazz.getInterfaces()) {
                getClassParents(c, type, result);
            }
        }
    }

    /**
     * Searches for an annotation on the given class, walking up the superclass chain and
     * all implemented interfaces recursively. Returns the first match found, or {@code null}.
     *
     * @param <T>        the annotation type
     * @param clazz      the class to start searching from
     * @param annotation the annotation class to look for
     * @return the annotation instance if found, or {@code null}
     */
    public static <T extends Annotation> T getClassAnnotation(Class<?> clazz, Class<T> annotation) {
        if (annotation != null && clazz != null) {
            if (clazz == Object.class) {
                return null;
            }

            T a = clazz.getAnnotation(annotation);
            if (a != null) {
                return a;
            }
            a = getClassAnnotation(clazz.getSuperclass(), annotation);
            if (a != null) {
                return a;
            }
            for (Class<?> c : clazz.getInterfaces()) {
                a = getClassAnnotation(c, annotation);
                if (a != null) {
                    return a;
                }
            }
        }
        return null;
    }

    /**
     * Collects all interfaces implemented by the given class and its entire superclass chain,
     * including transitively inherited interfaces. The result preserves insertion order.
     *
     * @param type the class to inspect
     * @return a set of all interfaces in the class hierarchy, in discovery order
     */
    public static Set<Class<?>> getAllInterfaces(Class<?> type) {
        Set<Class<?>> interfaces = new LinkedHashSet<>();

        while (type != null) {
            for (Class<?> iface : type.getInterfaces()) {
                collectInterfaces(iface, interfaces);
            }
            type = type.getSuperclass();
        }

        return interfaces;
    }

    private static void collectInterfaces(Class<?> iface, Set<Class<?>> interfaces) {
        if (interfaces.add(iface)) {
            for (Class<?> sub : iface.getInterfaces()) {
                collectInterfaces(sub, interfaces);
            }
        }
    }

    /**
     * Returns all superclasses of the given class, from the immediate parent up to
     * (and including) {@link Object}. Does not include the class itself.
     *
     * @param cls the class whose superclass chain to collect
     * @return a list of superclasses ordered from most specific to most general
     */
    public static List<Class<?>> getAllSuperclasses(Class<?> cls) {
        List<Class<?>> list = new ArrayList<>();

        Class<?> current = cls.getSuperclass();
        while (current != null) {
            list.add(current);
            current = current.getSuperclass();
        }

        return list;
    }

}
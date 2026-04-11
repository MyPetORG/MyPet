/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
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

import com.google.common.hash.Hashing;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import de.Keyle.MyPet.api.util.ErrorUtil;
import de.Keyle.MyPet.api.util.locale.Translation;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.*;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

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

    static Random rng = new Random();

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
     * Checks whether the given string can be parsed as a {@code byte}.
     *
     * @param number the string to test
     * @return {@code true} if the string is a valid byte ({@code -128} to {@code 127}), {@code false} otherwise
     */
    public static boolean isByte(String number) {
        try {
            Byte.parseByte(number);
            return true;
        } catch (NumberFormatException nFE) {
            return false;
        }
    }

    /**
     * Checks whether the given string can be parsed as a {@code double}.
     *
     * @param number the string to test
     * @return {@code true} if the string is a valid double, {@code false} otherwise
     */
    public static boolean isDouble(String number) {
        try {
            Double.parseDouble(number);
            return true;
        } catch (NumberFormatException nFE) {
            return false;
        }
    }

    /**
     * Converts an underscore-separated name into PascalCase.
     * Underscores are treated as word separators and removed, and the first letter of each
     * word is title-cased. For example, {@code "zombie_horse"} becomes {@code "ZombieHorse"}.
     *
     * @param name the underscore-separated name to capitalize, or {@code null}
     * @return the PascalCase name, or {@code null} if the input was {@code null}
     */
    public static String capitalizeName(String name) {
        if (name == null) {
            MyPetApi.getLogger().warning("Name is null");
            return null;
        }

        name = name.replace("_", " ");

        StringBuilder sb = new StringBuilder(name.length());
        boolean capitalizeNext = true;

        for (char c : name.toCharArray()) {
            if (Character.isLetter(c) && capitalizeNext) {
                sb.append(Character.toTitleCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
                capitalizeNext = !Character.isLetter(c);
            }
        }
        name = sb.toString();
        name = name.replace(" ", "");
        return name;
    }

    /**
     * Reads the entire contents of a file into a string using UTF-8 encoding.
     *
     * @param filePath the absolute or relative path to the file
     * @return the file contents as a string
     * @throws IOException if the file cannot be read
     */
    public static String readFileAsString(String filePath) throws IOException {
        return Files.readString(Path.of(filePath));
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
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeout))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(address))
                .timeout(Duration.ofMillis(timeout))
                .GET()
                .build();
        try {
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
     * Reads all bytes from an {@link InputStream} and decodes them into a string.
     * Any exception during reading is silently ignored, returning an empty string.
     *
     * @param is      the input stream to read
     * @param charset the charset to use for decoding
     * @return the decoded string, or an empty string if an error occurs
     */
    public static String toString(InputStream is, Charset charset) {
        try {
            return new String(is.readAllBytes(), charset);
        } catch (Exception ignored) {
        }
        return "";
    }

    /**
     * Checks whether a value falls within an inclusive range.
     *
     * @param intMin   the minimum bound (inclusive)
     * @param intMax   the maximum bound (inclusive)
     * @param intValue the value to test
     * @return {@code true} if {@code intMin <= intValue <= intMax}
     */
    public static boolean isBetween(int intMin, int intMax, int intValue) {
        return intValue >= intMin && intValue <= intMax;
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
     * Recursively searches a throwable's stack trace (including causes) for a class name
     * containing the given substring.
     *
     * @param throwable the throwable to search
     * @param string    the substring to match against class names in the stack trace
     * @return {@code true} if any stack frame's class name contains the string
     */
    public static boolean findStringInThrowable(Throwable throwable, String string) {
        for (StackTraceElement el : throwable.getStackTrace()) {
            if (el.getClassName().contains(string)) {
                return true;
            }
        }
        return throwable.getCause() != null && findStringInThrowable(throwable.getCause(), string);
    }

    /**
     * Builds a Kyori Adventure {@link HoverEvent} displaying a pet's statistics as a tooltip.
     * The tooltip includes hunger, HP/respawn time, experience, level, pet type, skill tree,
     * and dead status (if applicable). Values are highlighted in {@link NamedTextColor#GOLD}.
     *
     * @param mypet the stored pet whose stats to display
     * @param lang  the locale key for translating stat labels
     * @return a hover event that shows the formatted pet tooltip
     */
    public static HoverEvent<Component> myPetToItemHover(StoredMyPet mypet, String lang) {
        // Build component with proper colors matching the old RawMessage style
        TextComponent.Builder builder = Component.text();

        // Hunger stat
        builder.append(Translation.getComponent("Name.Hunger", lang))
                .append(Component.text(": "))
                .append(Component.text(Math.round(mypet.getSaturation())).color(NamedTextColor.GOLD))
                .append(Component.newline());

        // HP or Respawn time based on configuration
        if (!Configuration.Respawn.DISABLE_AUTO_RESPAWN) {
            if (mypet.getRespawnTime() > 0) {
                builder.append(Translation.getComponent("Name.Respawntime", lang))
                        .append(Component.text(": "))
                        .append(Component.text(mypet.getRespawnTime() + "sec").color(NamedTextColor.GOLD))
                        .append(Component.newline());
            } else {
                builder.append(Translation.getComponent("Name.HP", lang))
                        .append(Component.text(": "))
                        .append(Component.text(String.format("%1.2f", mypet.getHealth())).color(NamedTextColor.GOLD))
                        .append(Component.newline());
            }
        } else if (mypet.getRespawnTime() <= 0) {
            builder.append(Translation.getComponent("Name.HP", lang))
                    .append(Component.text(": "))
                    .append(Component.text(String.format("%1.2f", mypet.getHealth())).color(NamedTextColor.GOLD))
                    .append(Component.newline());
        }

        // Experience
        builder.append(Translation.getComponent("Name.Exp", lang))
                .append(Component.text(": "))
                .append(Component.text(String.format("%1.2f", mypet.getExp())).color(NamedTextColor.GOLD))
                .append(Component.newline());

        // Level (if available)
        if (mypet.getInfo().keySet().contains("storage")) {
            CompoundBinaryTag storage = mypet.getInfo().getCompound("storage");
            if (storage.keySet().contains("level")) {
                builder.append(Translation.getComponent("Name.Level", lang))
                        .append(Component.text(": "))
                        .append(Component.text(storage.getInt("level")).color(NamedTextColor.GOLD))
                        .append(Component.newline());
            }
        }

        // Pet Type - use client-side translatable for entity name
        String entityKey = "entity.minecraft." + mypet.getPetType().getBukkitName().toLowerCase();
        builder.append(Translation.getComponent("Name.Type", lang))
                .append(Component.text(": "))
                .append(Component.translatable(entityKey).color(NamedTextColor.GOLD))
                .append(Component.newline());

        // Skill tree
        builder.append(Translation.getComponent("Name.Skilltree", lang))
                .append(Component.text(": "))
                .append(Util.SANITIZED_MINIMESSAGE.deserialize(mypet.getSkilltree() != null ? mypet.getSkilltree().getDisplayName() : "-")
                        .color(NamedTextColor.GOLD));

        // Dead status (if applicable)
        if (Configuration.Respawn.DISABLE_AUTO_RESPAWN && mypet.getRespawnTime() > 0) {
            builder.append(Component.newline())
                    .append(Translation.getComponent("Name.Dead", lang).color(NamedTextColor.RED));
        }

        return HoverEvent.showText(builder.build());
    }

    /**
     * Captures the current thread's stack trace and formats it as a string.
     * Each frame is tab-indented and newline-separated.
     *
     * @return the formatted stack trace of the calling thread
     */
    public static String stackTraceToString() {
        StringBuilder trace = new StringBuilder();
        for (StackTraceElement e1 : Thread.currentThread().getStackTrace()) {
            trace.append("\t ").append(e1).append("\n");
        }
        return trace.toString();
    }

    /**
     * Computes the SHA-256 hash of a file and returns the first 8 bytes as a {@code long}.
     * Used as a change-detection token for file versioning (e.g. experience calculator scripts).
     *
     * @param file the file to hash
     * @return the truncated SHA-256 hash as a {@code long}, or {@code 0} if the file cannot be read
     */
    public static long getSha256FromFile(File file) {
        try {
            return com.google.common.io.Files.asByteSource(file).hash(Hashing.sha256()).asLong();
        } catch (IOException e) {
            ErrorUtil.report(e);
        }
        return 0;
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
     * Returns the fully-qualified name of a class, or {@code null} if the class is {@code null}.
     *
     * @param clazz the class, or {@code null}
     * @return the class name, or {@code null}
     */
    public static String getClassName(Class<?> clazz) {
        if (clazz != null) {
            return clazz.getName();
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
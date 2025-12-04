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

import com.google.common.base.Charsets;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.util.Colorizer;
import de.Keyle.MyPet.api.util.ErrorUtil;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagInt;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;

import java.io.*;
import java.lang.annotation.Annotation;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {

    static Random rng = new Random();

    public static Random getRandom() {
        return rng;
    }

    public static boolean isInt(String number) {
        try {
            Integer.parseInt(number);
            return true;
        } catch (NumberFormatException nFE) {
            return false;
        }
    }

    public static boolean isByte(String number) {
        try {
            Byte.parseByte(number);
            return true;
        } catch (NumberFormatException nFE) {
            return false;
        }
    }

    public static boolean isDouble(String number) {
        try {
            Double.parseDouble(number);
            return true;
        } catch (NumberFormatException nFE) {
            return false;
        }
    }

    public static boolean isLong(String number) {
        try {
            Long.parseLong(number);
            return true;
        } catch (NumberFormatException nFE) {
            return false;
        }
    }

    public static boolean isFloat(String number) {
        try {
            Float.parseFloat(number);
            return true;
        } catch (NumberFormatException nFE) {
            return false;
        }
    }

    public static boolean isShort(String number) {
        try {
            Short.parseShort(number);
            return true;
        } catch (NumberFormatException nFE) {
            return false;
        }
    }

    public static String cutString(String string, int length) {
        if (string.length() > length) {
            return string.substring(0, length - 1);
        }
        return string;
    }

    public static String formatText(String text, Object... values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null) {
                text = text.replaceAll("\\{" + i + "}", Matcher.quoteReplacement(values[i].toString()));
            }
        }
        return text;
    }

    // ========== Component-Based Formatting (Adventure API) ==========

    /**
     * Formats a Component by replacing placeholders {0}, {1}, {2}... with provided arguments.
     * Arguments can be Components (inserted with their styling) or Objects (converted to Components).
     * <p>
     * This is the modern replacement for formatText() that works with Adventure Components
     * and preserves formatting, colors, and styles.
     * <p>
     * Example:
     * <pre>
     * Component template = Component.text("Welcome, {0}! You have {1} points.");
     * Component result = Util.formatComponent(template,
     *     Component.text("Player").color(NamedTextColor.AQUA),
     *     Component.text("1234").color(NamedTextColor.GOLD)
     * );
     * </pre>
     *
     * @param component The template Component with placeholders {0}, {1}, etc.
     * @param values    Arguments to replace placeholders (can be Components or Objects)
     * @return New Component with placeholders replaced
     */
    public static Component formatComponent(Component component, Object... values) {
        if (component == null || values == null || values.length == 0) {
            return component != null ? component : Component.empty();
        }

        // Recursively process the component tree
        return replaceInComponent(component, values);
    }

    /**
     * Recursively processes a Component tree to replace placeholders.
     */
    private static Component replaceInComponent(Component component, Object[] values) {
        if (component == null) {
            return Component.empty();
        }

        // Start building the result
        TextComponent.Builder builder = Component.text();

        // If this is a TextComponent, process its content
        if (component instanceof TextComponent) {
            TextComponent textComponent = (TextComponent) component;
            String content = textComponent.content();

            // Replace placeholders in the content
            List<Component> replacedContent = replacePlaceholders(content, textComponent.style(), values);
            for (Component part : replacedContent) {
                builder.append(part);
            }
        } else {
            // For non-text components, preserve as-is
            builder.append(component);
        }

        // Process children recursively
        for (Component child : component.children()) {
            builder.append(replaceInComponent(child, values));
        }

        // Preserve the original component's styling (but not on the builder itself, applied to parts)
        return builder.build();
    }

    /**
     * Replaces placeholders in a text string and returns a list of Components.
     * If a placeholder is found, it's replaced with the corresponding value.
     * The resulting components preserve the original style.
     */
    private static List<Component> replacePlaceholders(String text, Style style, Object[] values) {
        List<Component> result = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return result;
        }

        int lastIndex = 0;
        Pattern pattern = Pattern.compile("\\{(\\d+)}");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            // Add text before the placeholder
            if (matcher.start() > lastIndex) {
                String beforeText = text.substring(lastIndex, matcher.start());
                result.add(Component.text(beforeText).style(style));
            }

            // Get the placeholder index
            int index = Integer.parseInt(matcher.group(1));

            // Replace with the corresponding value
            if (index < values.length && values[index] != null) {
                Object value = values[index];

                if (value instanceof Component) {
                    // Use Component argument directly
                    result.add((Component) value);
                } else {
                    // Convert to Component with the same style as the template
                    result.add(Component.text(value.toString()).style(style));
                }
            } else {
                // Placeholder not found in values, keep original
                result.add(Component.text(matcher.group()).style(style));
            }

            lastIndex = matcher.end();
        }

        // Add remaining text after the last placeholder
        if (lastIndex < text.length()) {
            String remainingText = text.substring(lastIndex);
            result.add(Component.text(remainingText).style(style));
        }

        // If no placeholders were found, return the original text as a component
        if (result.isEmpty() && !text.isEmpty()) {
            result.add(Component.text(text).style(style));
        }

        return result;
    }

    // ========== Translation + Formatting Convenience Methods ==========

    /**
     * Convenience method that combines Translation.getComponent() with formatComponent().
     * Gets a translated Component and replaces placeholders in one call.
     * <p>
     * Example:
     * <pre>
     * // Translation: Message.Welcome=<gold>Welcome, {0}! You have {1} points.
     * Component result = Util.formatTranslation("Message.Welcome", player,
     *     Component.text("Steve").color(NamedTextColor.AQUA),
     *     Component.text("1234").color(NamedTextColor.GOLD)
     * );
     * </pre>
     *
     * @param key    Translation key
     * @param player Player for language detection
     * @param values Arguments to replace placeholders
     * @return Formatted Component
     */
    public static Component formatTranslation(String key, org.bukkit.entity.Player player, Object... values) {
        Component template = Translation.getComponent(key, player);
        return formatComponent(template, values);
    }

    /**
     * Convenience method for CommandSender.
     */
    public static Component formatTranslation(String key, org.bukkit.command.CommandSender sender, Object... values) {
        Component template = Translation.getComponent(key, sender);
        return formatComponent(template, values);
    }

    /**
     * Convenience method for MyPetPlayer.
     */
    public static Component formatTranslation(String key, de.Keyle.MyPet.api.player.MyPetPlayer player, Object... values) {
        Component template = Translation.getComponent(key, player);
        return formatComponent(template, values);
    }

    /**
     * Convenience method for specific locale.
     */
    public static Component formatTranslation(String key, String localeString, Object... values) {
        Component template = Translation.getComponent(key, localeString);
        return formatComponent(template, values);
    }

    /**
     * Convenience method that combines Translation.getComponentMiniMessage() with formatComponent().
     * Gets a translated Component with MiniMessage support and replaces placeholders.
     * <p>
     * Example:
     * <pre>
     * // Translation: Message.Welcome=<gradient:gold:yellow>Welcome, {0}!</gradient>
     * Component result = Util.formatTranslationMiniMessage("Message.Welcome", player,
     *     Component.text("Steve").color(NamedTextColor.AQUA)
     * );
     * </pre>
     *
     * @param key    Translation key
     * @param player Player for language detection
     * @param values Arguments to replace placeholders
     * @return Formatted Component with MiniMessage formatting
     */
    public static Component formatTranslationMiniMessage(String key, org.bukkit.entity.Player player, Object... values) {
        Component template = Translation.getComponentMiniMessage(key, player);
        return formatComponent(template, values);
    }

    /**
     * Convenience method for CommandSender with MiniMessage.
     */
    public static Component formatTranslationMiniMessage(String key, org.bukkit.command.CommandSender sender, Object... values) {
        Component template = Translation.getComponentMiniMessage(key, sender);
        return formatComponent(template, values);
    }

    /**
     * Convenience method for MyPetPlayer with MiniMessage.
     */
    public static Component formatTranslationMiniMessage(String key, de.Keyle.MyPet.api.player.MyPetPlayer player, Object... values) {
        Component template = Translation.getComponentMiniMessage(key, player);
        return formatComponent(template, values);
    }

    /**
     * Convenience method for specific locale with MiniMessage.
     */
    public static Component formatTranslationMiniMessage(String key, String localeString, Object... values) {
        Component template = Translation.getComponentMiniMessage(key, localeString);
        return formatComponent(template, values);
    }

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

    public static String readFileAsString(String filePath) throws java.io.IOException {
        StringBuilder fileData = new StringBuilder(1000);
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        char[] buf = new char[1024];
        int numRead;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        reader.close();
        return fileData.toString();
    }

    public static String convertStreamToString(java.io.InputStream is) {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static String readUrlContent(String address) throws IOException {
        return readUrlContent(address, 2000);
    }

    public static String readUrlContent(String address, int timeout) throws IOException {
        StringBuilder contents = new StringBuilder(2048);
        BufferedReader br = null;

        try {
            URL url = new URL(address);
            HttpURLConnection huc = (HttpURLConnection) url.openConnection();
            huc.setConnectTimeout(timeout);
            huc.setReadTimeout(timeout);
            huc.setRequestMethod("GET");
            huc.connect();
            br = new BufferedReader(new InputStreamReader(huc.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                contents.append(line);
            }
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (Exception e) {
                ErrorUtil.report(e);
            }
        }
        return contents.toString();
    }

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

    public static String toString(InputStream is, Charset charset) {
        StringBuilder content = new StringBuilder();

        try {
            InputStreamReader in = new InputStreamReader(is, charset);
            int numBytes;
            final char[] buf = new char[512];
            while ((numBytes = in.read(buf)) != -1) {
                content.append(String.copyValueOf(buf, 0, numBytes));
            }
        } catch (Exception ignored) {
        }

        return content.toString();
    }

    /**
     * Compares two version strings.
     * <p>
     * Use this instead of String.compareTo() for a non-lexicographical
     * comparison that works for version strings. e.g. "1.10".compareTo("1.6").
     *
     * @param str1 a string of ordinal numbers separated by decimal points.
     * @param str2 a string of ordinal numbers separated by decimal points.
     * @return The result is a negative integer if str1 is _numerically_ less than str2.
     * The result is a positive integer if str1 is _numerically_ greater than str2.
     * The result is zero if the strings are _numerically_ equal.
     */
    public static int versionCompare(String str1, String str2) {
        String[] vals1 = str1.split("\\.");
        String[] vals2 = str2.split("\\.");
        if (vals1.length > vals2.length) {
            int oldLength = vals2.length;
            vals2 = Arrays.copyOf(vals2, vals1.length);
            for (int i = oldLength; i < vals1.length; i++) {
                vals2[i] = "0";
            }
        } else if (vals2.length > vals1.length) {
            int oldLength = vals1.length;
            vals1 = Arrays.copyOf(vals1, vals2.length);
            for (int i = oldLength; i < vals2.length; i++) {
                vals1[i] = "0";
            }
        }
        int i = 0;
        while (i < vals1.length - 1 && vals1[i].equals(vals2[i])) {
            i++;
        }
        if (i < vals1.length) {
            try {
                return Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    public static boolean isBetween(int intMin, int intMax, int intValue) {
        return intValue >= intMin && intValue <= intMax;
    }

    public static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    public static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    public static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    public static UUID getOfflinePlayerUUID(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(Charsets.UTF_8));
    }

    public static boolean findClassInStackTrace(StackTraceElement[] stackTrace, String className) {
        return findClassInStackTrace(stackTrace, className, 0, stackTrace.length - 1, false);
    }

    public static boolean findClassInStackTrace(StackTraceElement[] stackTrace, String className, int element) {
        return findClassInStackTrace(stackTrace, className, element, element, false);
    }

    public static boolean findClassInStackTrace(StackTraceElement[] stackTrace, String className, int from, int to, boolean debug) {
        if (to < from) {
            MyPetApi.getLogger().warning("\"to\" must be >= \"from\" (from=" + from + ", to=" + to + ")");
        }
        if (from < 0) {
            MyPetApi.getLogger().warning("\"from\" must be >= 0 (from=" + from + ")");
        }
        to = Math.min(stackTrace.length - 1, to);
        if (debug) {
            MyPetApi.getLogger().info("=====================================================================================================================================");
        }
        for (int i = from; i <= to; i++) {
            if (stackTrace[i].getClassName().equals(className)) {
                if (debug) {
                    MyPetApi.getLogger().info("=====================================================================================================================================");
                }
                return true;
            }
        }
        if (debug) {
            MyPetApi.getLogger().info("=====================================================================================================================================");
        }
        return false;
    }

    public static boolean findStringInThrowable(Throwable throwable, String string) {
        for (StackTraceElement el : throwable.getStackTrace()) {
            if (el.getClassName().contains(string)) {
                return true;
            }
        }
        return throwable.getCause() != null && findStringInThrowable(throwable.getCause(), string);
    }

    /**
     * Converts a StoredMyPet to a Kyori Adventure HoverEvent for displaying pet tooltips
     * Replacement for the old myPetToItemAction using RawMessage
     * Returns a hover event showing formatted pet statistics with colored values
     */
    public static HoverEvent<Component> myPetToItemHover(StoredMyPet mypet, String lang) {
        // Build component with proper colors matching the old RawMessage style
        TextComponent.Builder builder = Component.text();

        // Hunger stat
        builder.append(Component.text(Translation.getString("Name.Hunger", lang) + ": "))
                .append(Component.text(Math.round(mypet.getSaturation()))
                        .color(NamedTextColor.GOLD))
                .append(Component.newline());

        // HP or Respawn time based on configuration
        if (!Configuration.Respawn.DISABLE_AUTO_RESPAWN) {
            if (mypet.getRespawnTime() > 0) {
                builder.append(Component.text(Translation.getString("Name.Respawntime", lang) + ": "))
                        .append(Component.text(mypet.getRespawnTime() + "sec")
                                .color(NamedTextColor.GOLD))
                        .append(Component.newline());
            } else {
                builder.append(Component.text(Translation.getString("Name.HP", lang) + ": "))
                        .append(Component.text(String.format("%1.2f", mypet.getHealth()))
                                .color(NamedTextColor.GOLD))
                        .append(Component.newline());
            }
        } else if (mypet.getRespawnTime() <= 0) {
            builder.append(Component.text(Translation.getString("Name.HP", lang) + ": "))
                    .append(Component.text(String.format("%1.2f", mypet.getHealth()))
                            .color(NamedTextColor.GOLD))
                    .append(Component.newline());
        }

        // Experience
        builder.append(Component.text(Translation.getString("Name.Exp", lang) + ": "))
                .append(Component.text(String.format("%1.2f", mypet.getExp()))
                        .color(NamedTextColor.GOLD))
                .append(Component.newline());

        // Level (if available)
        if (mypet.getInfo().containsKey("storage")) {
            TagCompound storage = mypet.getInfo().getAs("storage", TagCompound.class);
            if (storage != null && storage.containsKey("level")) {
                builder.append(Component.text(Translation.getString("Name.Level", lang) + ": "))
                        .append(Component.text(storage.getAs("level", TagInt.class).getIntData())
                                .color(NamedTextColor.GOLD))
                        .append(Component.newline());
            }
        }

        // Pet Type - use client-side translatable for entity name
        String entityKey = "entity.minecraft." + mypet.getPetType().getBukkitName().toLowerCase();
        builder.append(Component.text(Translation.getString("Name.Type", lang) + ": "))
                .append(Component.translatable(entityKey)
                        .color(NamedTextColor.GOLD))
                .append(Component.newline());

        // Skill tree
        String skilltreeName = mypet.getSkilltree() != null ? Colorizer.setColors(mypet.getSkilltree().getDisplayName()) : "-";
        builder.append(Component.text(Translation.getString("Name.Skilltree", lang) + ": "))
                .append(Component.text(skilltreeName)
                        .color(NamedTextColor.GOLD));

        // Dead status (if applicable)
        if (Configuration.Respawn.DISABLE_AUTO_RESPAWN && mypet.getRespawnTime() > 0) {
            builder.append(Component.newline())
                    .append(Component.text(Translation.getString("Name.Dead", lang))
                            .color(NamedTextColor.RED));
        }

        return HoverEvent.showText(builder.build());
    }

    public static String stackTraceToString() {
        StringBuilder trace = new StringBuilder();
        for (StackTraceElement e1 : Thread.currentThread().getStackTrace()) {
            trace.append("\t ").append(e1).append("\n");
        }
        return trace.toString();
    }

    public static int getJavaUpdate() {
        try {
            String[] javaVersionElements = System.getProperty("java.runtime.version").split("\\.|_|-b");
            return Integer.parseInt(javaVersionElements[3]);
        } catch (Exception e) {
            return -1;
        }
    }

    public static String getFileExtension(String fileName) {

        String extension = "";

        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i + 1);
        }

        return extension;
    }

    public static long getSha256FromFile(File file) {
        try {
            Hasher hasher = Hashing.sha256().newHasher();
            BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(file.toPath()));
            byte[] buf = new byte[1024];
            int numRead;
            while ((numRead = bis.read(buf)) != -1) {
                hasher.putBytes(buf, 0, numRead);
            }
            bis.close();
            return hasher.hash().asLong();
        } catch (IOException e) {
            ErrorUtil.report(e);
        }
        return 0;
    }

    public static <T> void getClassParents(Class clazz, Class<T> type, Set<Class<? extends T>> result) {
        if (type != null && clazz != null && result != null && clazz != type) {
            if (clazz == Object.class) {
                return;
            }
            if (type.isAssignableFrom(clazz)) {
                //noinspection unchecked
                result.add(clazz);
            }
            getClassParents(clazz.getSuperclass(), type, result);
            for (Class c : clazz.getInterfaces()) {
                getClassParents(c, type, result);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Annotation> T getClassAnnotation(Class clazz, Class<T> annotation) {
        if (annotation != null && clazz != null) {
            if (clazz == Object.class) {
                return null;
            }

            T a = (T) clazz.getAnnotation(annotation);
            if (a != null) {
                return a;
            }
            a = getClassAnnotation(clazz.getSuperclass(), annotation);
            if (a != null) {
                return a;
            }
            for (Class c : clazz.getInterfaces()) {
                a = getClassAnnotation(c, annotation);
                if (a != null) {
                    return a;
                }
            }
        }
        return null;
    }

    public static String escapeJsonString(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); ++i) {
            char ch = s.charAt(i);
            switch (ch) {
                case '\b':
                    sb.append("\\b");
                    continue;
                case '\t':
                    sb.append("\\t");
                    continue;
                case '\n':
                    sb.append("\\n");
                    continue;
                case '\f':
                    sb.append("\\f");
                    continue;
                case '\r':
                    sb.append("\\r");
                    continue;
                case '"':
                    sb.append("\\\"");
                    continue;
                case '/':
                    sb.append("\\/");
                    continue;
                case '\\':
                    sb.append("\\\\");
                    continue;
            }

            if (ch <= 31 || ch >= 127 && ch <= 159 || ch >= 8192 && ch <= 8447) {
                String ss = Integer.toHexString(ch);
                sb.append("\\u");

                sb.append("0".repeat(4 - ss.length()));

                sb.append(ss.toUpperCase());
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    public static String getClassName(Class clazz) {
        if (clazz != null) {
            return clazz.getName();
        }
        return null;
    }

    public static boolean stringsEqual(String a, String b, boolean ignoreCase) {
        if (a == null) {
            return b == null;
        }
        if (b == null) {
            return a == null;
        }
        return ignoreCase ? a.equalsIgnoreCase(b) : a.equals(b);
    }

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
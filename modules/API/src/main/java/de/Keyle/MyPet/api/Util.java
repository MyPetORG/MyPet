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
import de.Keyle.MyPet.api.util.chat.parts.ItemTooltip;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.keyle.knbt.TagCompound;
import de.keyle.knbt.TagInt;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;

import java.io.*;
import java.lang.annotation.Annotation;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;

import static org.bukkit.ChatColor.GOLD;
import static org.bukkit.ChatColor.RESET;

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

    public static String capitalizeName(String name) {
        Validate.notNull(name, "Name can't be null");

        name = name.replace("_", " ");
        name = WordUtils.capitalizeFully(name);
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
                e.printStackTrace();
            }
        }
        return contents.toString();
    }

    public static String decimal2roman(int src) {
        char digits[] = {'I', 'V', 'X', 'L', 'C', 'D', 'M'};
        String thousands = "", result = "";
        int rang, digit, i;

        for (i = src / 1000; i > 0; i--) {
            thousands += "M";
        }
        src %= 1000;

        rang = 0;
        while (src > 0) {
            digit = src % 10;
            src /= 10;
            switch (digit) {
                case 1:
                    result = "" + digits[rang] + result;
                    break;
                case 2:
                    result = "" + digits[rang] + digits[rang] + result;
                    break;
                case 3:
                    result = "" + digits[rang] + digits[rang] + digits[rang] + result;
                    break;
                case 4:
                    result = "" + digits[rang] + digits[rang + 1] + result;
                    break;
                case 5:
                    result = "" + digits[rang + 1] + result;
                    break;
                case 6:
                    result = "" + digits[rang + 1] + digits[rang] + result;
                    break;
                case 7:
                    result = "" + digits[rang + 1] + digits[rang] + digits[rang] + result;
                    break;
                case 8:
                    result = "" + digits[rang + 1] + digits[rang] + digits[rang] + digits[rang] + result;
                    break;
                case 9:
                    result = "" + digits[rang] + digits[rang + 2] + result;
                    break;
            }
            rang += 2;
        }
        return thousands + result;
    }

    public static String toString(InputStream is, Charset charset) {
        String content = "";

        try {
            InputStreamReader in = new InputStreamReader(is, charset);
            int numBytes;
            final char[] buf = new char[512];
            while ((numBytes = in.read(buf)) != -1) {
                content += String.copyValueOf(buf, 0, numBytes);
            }
        } catch (Exception ignored) {
        }

        return content;
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
        Validate.isTrue(to >= from, "\"to\" has to be >= \"from\".");
        Validate.isTrue(from >= 0, "\"from\" has to be >= 0.");
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

    public static ItemTooltip myPetToItemTooltip(StoredMyPet mypet, String lang) {
        List<String> lore = new ArrayList<>();
        lore.add(RESET + Translation.getString("Name.Hunger", lang) + ": " + GOLD + Math.round(mypet.getSaturation()));
        if (!Configuration.Respawn.DISABLE_AUTO_RESPAWN) {
            if (mypet.getRespawnTime() > 0) {
                lore.add(RESET + Translation.getString("Name.Respawntime", lang) + ": " + GOLD + mypet.getRespawnTime() + "sec");
            } else {
                lore.add(RESET + Translation.getString("Name.HP", lang) + ": " + GOLD + String.format("%1.2f", mypet.getHealth()));
            }
        } else if (mypet.getRespawnTime() <= 0) {
            lore.add(RESET + Translation.getString("Name.HP", lang) + ": " + GOLD + String.format("%1.2f", mypet.getHealth()));
        }
        lore.add(RESET + Translation.getString("Name.Exp", lang) + ": " + GOLD + String.format("%1.2f", mypet.getExp()));
        if (mypet.getInfo().containsKey("storage")) {
            TagCompound storage = mypet.getInfo().getAs("storage", TagCompound.class);
            if (storage.containsKey("level")) {
                lore.add(RESET + Translation.getString("Name.Level", lang) + ": " + GOLD + storage.getAs("level", TagInt.class).getIntData());
            }
        }
        lore.add(RESET + Translation.getString("Name.Type", lang) + ": " + GOLD + mypet.getPetType().name());
        lore.add(RESET + Translation.getString("Name.Skilltree", lang) + ": " + GOLD + (mypet.getSkilltree() != null ? Colorizer.setColors(mypet.getSkilltree().getDisplayName()) : "-"));
        if (Configuration.Respawn.DISABLE_AUTO_RESPAWN) {
            if (mypet.getRespawnTime() > 0) {
                lore.add(ChatColor.RED + Translation.getString("Name.Dead", lang));
            }
        }

        return new ItemTooltip().addLore(lore).setTitle(mypet.getPetName());
    }

    public static String stackTraceToString() {
        String trace = "";
        for (StackTraceElement e1 : Thread.currentThread().getStackTrace()) {
            trace += "\t " + e1.toString() + "\n";
        }
        return trace;
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
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            byte[] buf = new byte[1024];
            int numRead;
            while ((numRead = bis.read(buf)) != -1) {
                hasher.putBytes(buf, 0, numRead);
            }
            bis.close();
            return hasher.hash().asLong();
        } catch (IOException e) {
            e.printStackTrace();
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

                for (int k = 0; k < 4 - ss.length(); ++k) {
                    sb.append('0');
                }

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
}
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

package de.Keyle.MyPet.api;

import com.google.common.base.Charsets;
import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.StoredMyPet;
import de.Keyle.MyPet.api.util.ReflectionUtil;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.keyle.fanciful.ItemTooltip;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Material;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;

import static org.bukkit.ChatColor.GOLD;
import static org.bukkit.ChatColor.RESET;

public class Util {
    @Deprecated
    public static Method getMethod(Class<?> clazz, String method, Class<?>... parameterTypes) {
        return ReflectionUtil.getMethod(clazz, method, parameterTypes);
    }

    @Deprecated
    public static Field getField(Class<?> clazz, String field) {
        return ReflectionUtil.getField(clazz, field);
    }

    @Deprecated
    public static boolean setFieldValue(Field field, Object object, Object value) {
        return ReflectionUtil.setFieldValue(field, object, value);
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
            return string.substring(0, length);
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
            return Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
        }
        return 0;
    }

    public static boolean isBetween(int intMin, int intMax, int intValue) {
        return intValue >= intMin && intValue <= intMax;
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

    public static ItemTooltip myPetToItemTooltip(StoredMyPet mypet, String lang) {
        List<String> lore = new ArrayList<>();
        lore.add(RESET + Translation.getString("Name.Hunger", lang) + ": " + GOLD + Math.round(mypet.getHungerValue()));
        if (mypet.getRespawnTime() > 0) {
            lore.add(RESET + Translation.getString("Name.Respawntime", lang) + ": " + GOLD + mypet.getRespawnTime() + "sec");
        } else {
            lore.add(RESET + Translation.getString("Name.HP", lang) + ": " + GOLD + String.format("%1.2f", mypet.getHealth()));
        }
        lore.add(RESET + Translation.getString("Name.Exp", lang) + ": " + GOLD + String.format("%1.2f", mypet.getExp()));
        lore.add(RESET + Translation.getString("Name.Type", lang) + ": " + GOLD + mypet.getPetType().name());
        lore.add(RESET + Translation.getString("Name.Skilltree", lang) + ": " + GOLD + (mypet.getSkilltree() != null ? mypet.getSkilltree().getDisplayName() : "-"));

        return new ItemTooltip().setMaterial(Material.MONSTER_EGG).addLore(lore).setTitle(mypet.getPetName());
    }
}
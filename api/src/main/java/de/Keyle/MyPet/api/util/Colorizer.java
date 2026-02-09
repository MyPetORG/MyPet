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

package de.Keyle.MyPet.api.util;

import org.bukkit.ChatColor;

import java.util.HashMap;
import java.util.Map;

public class Colorizer {

    private static Map<String, String> colorCodes = new HashMap<>();
    private static final Map<Character, String> ANSI_CODES = new HashMap<>();

    public static String setColors(String text) {
        for (String color : colorCodes.keySet()) {
            text = text.replaceAll("(?i)<" + color + ">", ChatColor.COLOR_CHAR + colorCodes.get(color));
        }
        text = text.replaceAll("(?i)<([0-9a-fk-or])>", ChatColor.COLOR_CHAR + "$1");
        text = ChatColor.translateAlternateColorCodes('&', text);
        return text;
    }

    public static String stripColors(String text) {
        for (String color : colorCodes.keySet()) {
            text = text.replaceAll("(?i)<" + color + ">", "");
        }
        text = text.replaceAll("(?i)<[0-9a-fk-or]>", "");
        text = ChatColor.stripColor(text);
        return text;
    }

    public static String toAnsi(String text) {
        if (text == null || text.indexOf(ChatColor.COLOR_CHAR) == -1) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text.length());
        boolean converted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ChatColor.COLOR_CHAR && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                String ansi = ANSI_CODES.get(code);
                if (ansi != null) {
                    sb.append(ansi);
                    converted = true;
                    i++;
                    continue;
                }
            }
            sb.append(c);
        }
        if (converted) {
            sb.append("\u001b[m");
        }
        return sb.toString();
    }

    static {
        for (ChatColor color : ChatColor.values()) {
            colorCodes.put(color.name().replace("_", ""), String.valueOf(color.getChar()));
            colorCodes.put(color.name(), String.valueOf(color.getChar()));
        }

        // Colors
        ANSI_CODES.put('0', "\u001b[30m");   // BLACK
        ANSI_CODES.put('1', "\u001b[34m");   // DARK_BLUE
        ANSI_CODES.put('2', "\u001b[32m");   // DARK_GREEN
        ANSI_CODES.put('3', "\u001b[36m");   // DARK_AQUA
        ANSI_CODES.put('4', "\u001b[31m");   // DARK_RED
        ANSI_CODES.put('5', "\u001b[35m");   // DARK_PURPLE
        ANSI_CODES.put('6', "\u001b[33m");   // GOLD
        ANSI_CODES.put('7', "\u001b[37m");   // GRAY
        ANSI_CODES.put('8', "\u001b[90m");   // DARK_GRAY
        ANSI_CODES.put('9', "\u001b[94m");   // BLUE
        ANSI_CODES.put('a', "\u001b[92m");   // GREEN
        ANSI_CODES.put('b', "\u001b[96m");   // AQUA
        ANSI_CODES.put('c', "\u001b[91m");   // RED
        ANSI_CODES.put('d', "\u001b[95m");   // LIGHT_PURPLE
        ANSI_CODES.put('e', "\u001b[93m");   // YELLOW
        ANSI_CODES.put('f', "\u001b[97m");   // WHITE
        // Formatting
        ANSI_CODES.put('k', "\u001b[8m");    // OBFUSCATED
        ANSI_CODES.put('l', "\u001b[1m");    // BOLD
        ANSI_CODES.put('m', "\u001b[9m");    // STRIKETHROUGH
        ANSI_CODES.put('n', "\u001b[4m");    // UNDERLINE
        ANSI_CODES.put('o', "\u001b[3m");    // ITALIC
        ANSI_CODES.put('r', "\u001b[m");     // RESET
    }
}
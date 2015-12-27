/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2016 Keyle
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

package de.Keyle.MyPet.util;

import org.bukkit.ChatColor;

import java.util.HashMap;
import java.util.Map;

public class Colorizer {
    private static Map<String, String> colorCodes = new HashMap<>();

    public static String setColors(String text) {
        for (String color : colorCodes.keySet()) {
            text = text.replaceAll("(?i)<" + color + ">", ChatColor.COLOR_CHAR + colorCodes.get(color));
        }
        text = text.replaceAll("(?i)<([0-9a-fk-or])>", ChatColor.COLOR_CHAR + "$1");
        return text;
    }

    static {
        for (ChatColor color : ChatColor.values()) {
            colorCodes.put(color.name().replace("_", ""), String.valueOf(color.getChar()));
        }
    }
}
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

import de.Keyle.MyPet.MyPetApi;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NameFilter {
    public static List<String> NAME_FILTER = new ArrayList<>();

    public static boolean isClean(String text) {
        text = Colorizer.stripColors(text);
        for (String pattern : NAME_FILTER) {
            try {
                if (findRegEx(pattern, text)) {
                    return false;
                }
            } catch (Exception ignored) {
                MyPetApi.getLogger().info("This name filter pattern caused a problem: " + pattern);
            }
        }
        return true;
    }

    private static boolean findRegEx(String pattern, String text) {
        Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher regexMatcher = regex.matcher(text);
        return regexMatcher.find();
    }
}
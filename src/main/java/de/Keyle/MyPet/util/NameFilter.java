/*
 * This file is part of MyPet-1.8
 *
 * Copyright (C) 2011-2015 Keyle
 * MyPet-1.8 is licensed under the GNU Lesser General Public License.
 *
 * MyPet-1.8 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet-1.8 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.util;

import com.google.common.collect.Lists;

import java.util.List;

public class NameFilter {
    public static List<String> NAME_FILTER = Lists.newArrayList();

    public static String clean(String text) {
        for (String pattern : NAME_FILTER) {
            text = text.replaceAll(pattern, "*");
        }
        return text;
    }
}
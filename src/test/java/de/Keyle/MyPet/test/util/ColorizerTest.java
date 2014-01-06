/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2014 Keyle
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

package de.Keyle.MyPet.test.util;

import de.Keyle.MyPet.util.Colorizer;
import org.bukkit.ChatColor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ColorizerTest {
    @Test
    public void testColorizer() {
        String originalText = "";
        String originalNameText = "";
        String expectedResultText = "";

        for (ChatColor color : ChatColor.values()) {
            expectedResultText += ChatColor.COLOR_CHAR + String.valueOf(color.getChar());
            originalText += "<" + String.valueOf(color.getChar()) + ">";
            originalNameText += "<" + color.name().replace("_", "") + ">";
        }

        assertEquals(Colorizer.setColors(originalText), expectedResultText);
        assertEquals(Colorizer.setColors(originalNameText), expectedResultText);
    }
}
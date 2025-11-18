/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2025 Keyle
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

package de.Keyle.MyPet;

import java.awt.Desktop;
import java.net.URI;

public class Main {
    static String url = "https://skilltree.mypet-plugin.de";
    public static void main(String[] args) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(url));
                    System.out.println("Opening MyPet Skilltree Editor in your default browser...");
            } else {
                System.err.println("Failed to open Skilltree Editor.");
                System.out.println("Please visit: " + url);
            }
        } catch (Exception e) {
            System.err.println("Failed to open Skilltree Editor: " + e.getMessage());
            System.out.println("Please visit: " + url);
        }
    }
}

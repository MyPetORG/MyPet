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
    public static void main(String[] args) {
        try {
            String url = "https://skilltree.mypet-plugin.de";

            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(new URI(url));
                    System.out.println("Opening MyPet Skilltree Editor in your default browser...");
                    System.out.println("URL: " + url);
                } else {
                    System.err.println("Desktop browsing is not supported on this system.");
                    System.out.println("Please visit: " + url);
                }
            } else {
                System.err.println("Desktop is not supported on this system.");
                System.out.println("Please visit: " + url);
            }
        } catch (Exception e) {
            System.err.println("Failed to open browser: " + e.getMessage());
            System.out.println("Please visit: https://skilltree.mypet-plugin.de");
        }
    }
}

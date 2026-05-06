/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2026 Keyle
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

import org.jetbrains.annotations.NotNull;

import java.awt.Desktop;
import java.net.URI;

/**
 * Executable entry point for the MyPet JAR when run outside of a Minecraft server.
 *
 * <p>Since MyPet is a Bukkit/Paper plugin, it is not intended to be run as a standalone
 * application. When a user double-clicks or executes the JAR directly, this class
 * redirects them to the MyPet Skilltree Editor website instead of failing silently.</p>
 */
public class Main {

    /** URL of the MyPet Skilltree Editor web application. */
    @NotNull
    private static final String SKILLTREE_EDITOR_URL = "https://skilltree.mypet-plugin.de";

    /**
     * Opens the MyPet Skilltree Editor in the user's default browser.
     *
     * <p>If the desktop environment supports browsing, the URL is opened automatically.
     * Otherwise, the URL is printed to the console for manual navigation.</p>
     *
     * @param args command-line arguments (unused)
     */
    public static void main(@NotNull String[] args) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(SKILLTREE_EDITOR_URL));
                System.out.println("Opening MyPet Skilltree Editor in your default browser...");
            } else {
                System.err.println("Failed to open Skilltree Editor.");
                System.out.println("Please visit: " + SKILLTREE_EDITOR_URL);
            }
        } catch (Exception e) {
            System.err.println("Failed to open Skilltree Editor: " + e.getMessage());
            System.out.println("Please visit: " + SKILLTREE_EDITOR_URL);
        }
    }
}

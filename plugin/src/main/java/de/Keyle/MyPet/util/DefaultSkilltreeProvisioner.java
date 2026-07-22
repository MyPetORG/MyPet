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

package de.Keyle.MyPet.util;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.ErrorUtil;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Seeds the {@code skilltrees/} folder of a fresh installation with the bundled default
 * {@code .st.json} files (tier-1 paths, path ascensions, and species signatures). The set of
 * bundled files is discovered from the plugin JAR at runtime rather than hardcoded, so adding,
 * renaming, or removing a bundled skilltree needs no change here. Existing files are never
 * overwritten, and the copy only runs on first-time setup.
 */
public final class DefaultSkilltreeProvisioner {

    private static final String RESOURCE_DIR = "skilltrees/";
    private static final String SUFFIX = ".st.json";

    private DefaultSkilltreeProvisioner() {
    }

    /**
     * Copies the bundled default skilltree JSON files into {@code skilltreeFolder} if and only if
     * the folder did not already exist (i.e. {@code createdFolder} is true). Existing files are
     * never overwritten. Logs a single info line on completion when files are copied.
     *
     * @param skilltreeFolder the destination folder (typically {@code <pluginDataFolder>/skilltrees})
     * @param createdFolder   {@code true} if the caller's most recent {@code skilltreeFolder.mkdirs()}
     *                        actually created the folder; {@code false} if it already existed
     * @param plugin          the plugin instance whose JAR contains the bundled resources
     */
    public static void copyDefaultsIfFolderCreated(@NotNull File skilltreeFolder,
                                                   boolean createdFolder,
                                                   @NotNull Plugin plugin) {
        if (!createdFolder) {
            return;
        }
        int copied = 0;
        for (String fileName : bundledSkilltreeFileNames(plugin)) {
            File target = new File(skilltreeFolder, fileName);
            if (!target.exists() && ResourceUtil.copyResource(plugin, RESOURCE_DIR + fileName, target)) {
                copied++;
            }
        }
        if (copied > 0) {
            MyPetApi.getLogger().info("Default skilltree files created (" + copied + ").");
        }
    }

    /** File names of every bundled {@code skilltrees/<name>.st.json} resource, from the plugin JAR (or exploded classpath in dev). */
    private static List<String> bundledSkilltreeFileNames(@NotNull Plugin plugin) {
        List<String> names = new ArrayList<>();
        try {
            File codeSource = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            if (codeSource.isDirectory()) {
                // Exploded classpath (dev / IDE run): the resources sit on disk.
                File[] files = new File(codeSource, RESOURCE_DIR).listFiles((dir, name) -> name.endsWith(SUFFIX));
                if (files != null) {
                    for (File file : files) {
                        names.add(file.getName());
                    }
                }
            } else {
                try (JarFile jar = new JarFile(codeSource)) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        String entryName = entries.nextElement().getName();
                        // Only files directly under skilltrees/ (no nested subdirectories).
                        if (entryName.startsWith(RESOURCE_DIR) && entryName.endsWith(SUFFIX)
                                && entryName.indexOf('/', RESOURCE_DIR.length()) < 0) {
                            names.add(entryName.substring(RESOURCE_DIR.length()));
                        }
                    }
                }
            }
        } catch (Exception e) {
            ErrorUtil.report(e);
        }
        names.sort(null);
        return names;
    }
}

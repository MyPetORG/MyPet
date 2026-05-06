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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

public class VersionUtil {
    private static boolean updated = false;

    private static String version = "0.0.0";
    private static String build = "";
    private static String buildType = "local";

    private static void loadData() {
        try {
            // Get version from plugin.yml via Bukkit's plugin description
            version = MyPetApi.getPlugin().getDescription().getVersion();

            // Get other metadata from JAR manifest
            String path = VersionUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            Attributes attr = getManifestAttributes(path);

            String val;
            if ((val = attr.getValue("Project-Version")) != null && !val.isEmpty()) {
                version = val;
            }
            if ((val = attr.getValue("Project-Build")) != null && !val.isEmpty()) {
                build = val;
            }
            if ((val = attr.getValue("Project-Type")) != null && !val.isEmpty()) {
                buildType = val;
            }
        } catch (IOException | URISyntaxException e) {
            ErrorUtil.report(e);
        }
    }

    private static Attributes getManifestAttributes(String filepath) throws IOException {
        try (JarFile jf = new JarFile(new File(filepath))) {
            return jf.getManifest().getMainAttributes();
        }
    }

    public static String getVersion() {
        if (!updated) {
            loadData();
            updated = true;
        }
        return version;
    }

    public static boolean isDevBuild() {
        String v = getVersion();
        return v.contains("-alpha") || v.contains("-beta");
    }

    public static boolean isLocalBuild() {
        return getVersion().endsWith("-local");
    }

    public static String getBuild() {
        if (!updated) {
            loadData();
            updated = true;
        }
        return build;
    }

    public static String getFormattedVersion() {
        return getVersion();
    }

    public static void reset() {
        updated = false;
    }
}
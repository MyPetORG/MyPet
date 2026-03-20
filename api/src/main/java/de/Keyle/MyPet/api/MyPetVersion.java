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

package de.Keyle.MyPet.api;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.util.ErrorUtil;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class MyPetVersion {
    private static boolean updated = false;

    private static String version = "0.0.0";
    private static String build = "";
    private static String buildType = "local";
    private static String minecraftVersion = "0.0.0";
    private static List<String> bukkitPackets = new ArrayList<>();

    private static void loadData() {
        try {
            // Get version from plugin.yml via Bukkit's plugin description
            version = MyPetApi.getPlugin().getDescription().getVersion();

            // Get other metadata from JAR manifest
            String path = MyPetVersion.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
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
            if (attr.getValue("Project-Minecraft-Version") != null) {
                minecraftVersion = attr.getValue("Project-Minecraft-Version");
            }
            if (attr.getValue("Project-Bukkit-Packets") != null) {
                String bukkitPackets = attr.getValue("Project-Bukkit-Packets");
                MyPetVersion.bukkitPackets.clear();
                Collections.addAll(MyPetVersion.bukkitPackets, bukkitPackets.split(";"));
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
        return getVersion().contains("SNAPSHOT");
    }

    public static boolean isLocalBuild() {
        if (!updated) {
            loadData();
            updated = true;
        }
        return "local".equalsIgnoreCase(buildType);
    }

    public static String getBuild() {
        if (!updated) {
            loadData();
            updated = true;
        }
        return build;
    }

    public static String getFormattedVersion() {
        String result = getVersion();
        String b = getBuild();
        if (b != null && !b.isEmpty()) {
            boolean numeric = b.chars().allMatch(Character::isDigit);
            result += "-" + (numeric ? "b" : "") + b;
        }
        return result;
    }

    public static String getMinecraftVersion() {
        if (!updated) {
            loadData();
            updated = true;
        }
        return minecraftVersion;
    }

    public static boolean isValidBukkitPacket(String p1) {
        if (!updated) {
            loadData();
            updated = true;
        }
        for (String p2 : bukkitPackets) {
            if (p1.equals(p2)) {
                return true;
            }
        }
        return false;
    }

    public static List<String> getBukkitPackets() {
        if (!updated) {
            loadData();
            updated = true;
        }
        return Collections.unmodifiableList(bukkitPackets);
    }

    public static void reset() {
        updated = false;
    }
}
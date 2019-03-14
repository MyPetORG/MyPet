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
    private static String build = "0";
    private static String minecraftVersion = "0.0.0";
    private static List<String> bukkitPackets = new ArrayList<>();

    private static void loadData() {
        try {
            String path = MyPetVersion.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            Attributes attr = getClassLoaderForExtraModule(path).getMainAttributes();

            if (attr.getValue("Project-Version") != null) {
                version = attr.getValue("Project-Version");
            }
            if (attr.getValue("Project-Build") != null) {
                build = attr.getValue("Project-Build");
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
            e.printStackTrace();
        }
    }

    private static Manifest getClassLoaderForExtraModule(String filepath) throws IOException {
        File jar = new File(filepath);
        JarFile jf = new JarFile(jar);
        Manifest mf = jf.getManifest();
        jf.close();
        return mf;
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

    public static String getBuild() {
        if (!updated) {
            loadData();
            updated = true;
        }
        return build;
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
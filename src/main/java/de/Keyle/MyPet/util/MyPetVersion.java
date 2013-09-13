/*
 * This file is part of MyPet
 *
 * Copyright (C) 2011-2013 Keyle
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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class MyPetVersion {
    private static boolean updated = false;

    private static String myPetVersion = "0.0.0";
    private static String myPetBuild = "0";
    private static String minecraftVersion = "0.0.0";

    private static void getManifestVersion() {
        try {
            String path = MyPetVersion.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            Attributes attr = getClassLoaderForExtraModule(path).getMainAttributes();

            if (attr.getValue("Project-Version") != null) {
                myPetVersion = attr.getValue("Project-Version");
            }
            if (attr.getValue("Project-Build") != null) {
                myPetBuild = attr.getValue("Project-Build");
            }
            if (attr.getValue("Project-Minecraft-Version") != null) {
                minecraftVersion = attr.getValue("Project-Minecraft-Version");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
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

    public static String getMyPetVersion() {
        if (!updated) {
            getManifestVersion();
            updated = true;
        }
        return myPetVersion;
    }

    public static String getMyPetBuild() {
        if (!updated) {
            getManifestVersion();
            updated = true;
        }
        return myPetBuild;
    }

    public static String getMinecraftVersion() {
        if (!updated) {
            getManifestVersion();
            updated = true;
        }
        return minecraftVersion;
    }

    public static void reset() {
        updated = false;
    }
}
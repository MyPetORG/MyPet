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

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class MyPetVersion
{
    private static boolean updated = false;

    private static String MyPetVersion = "0.0.0";
    private static String MyPetBuild = "0";
    private static String MinecraftVersion = "0.0.0";

    private static void getManifestVersion()
    {
        try
        {
            Enumeration<URL> e = MyPetVersion.class.getClassLoader().getResources(JarFile.MANIFEST_NAME);
            while(e.hasMoreElements())
            {
                URL u = e.nextElement();

                if(u.getPath().contains(MyPetVersion.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()))
                {
                    Manifest mf = new Manifest(u.openStream());
                    Attributes attr = mf.getMainAttributes();

                    if(attr.getValue("Project-Version") != null)
                    {
                        MyPetVersion = attr.getValue("Project-Version");
                    }
                    if(attr.getValue("Project-Build") != null)
                    {
                        MyPetBuild = attr.getValue("Project-Build");
                    }
                    if(attr.getValue("Project-Minecraft-Version") != null)
                    {
                        MinecraftVersion = attr.getValue("Project-Minecraft-Version");
                    }
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (URISyntaxException e)
        {
            e.printStackTrace();
        }
    }

    public static String getMyPetVersion()
    {
        if(!updated)
        {
            getManifestVersion();
            updated = true;
        }
        return MyPetVersion;
    }

    public static String getMyPetBuild()
    {
        if(!updated)
        {
            getManifestVersion();
            updated = true;
        }
        return MyPetBuild;
    }

    public static String getMinecraftVersion()
    {
        if(!updated)
        {
            getManifestVersion();
            updated = true;
        }
        return MinecraftVersion;
    }

    public static void reset()
    {
        updated = false;
    }
}

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

package de.Keyle.MyPet.util.locale;

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.util.Colorizer;
import de.Keyle.MyPet.util.MyPetBukkitUtil;
import de.Keyle.MyPet.util.MyPetPlayer;
import de.Keyle.MyPet.util.MyPetUtil;
import org.apache.commons.lang.LocaleUtils;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class MyPetLocales
{
    private static MyPetLocales latestMyPetLocales = null;

    private Map<String, MyPetResourceBundle> locales = new HashMap<String, MyPetResourceBundle>();
    private JarFile jarFile;

    public MyPetLocales()
    {
        File pluginFile = MyPetPlugin.getPlugin().getFile();
        try
        {
            jarFile = new JarFile(pluginFile);
        }
        catch (IOException ignored)
        {
            jarFile = null;
        }
        loadLocale("en");
        latestMyPetLocales = this;
    }

    public static String getString(String key, Player player)
    {
        if (player == null)
        {
            return key;
        }

        return getString(key, MyPetBukkitUtil.getPlayerLanguage(player));
    }

    public static String getString(String key, MyPetPlayer player)
    {
        if (player == null)
        {
            return key;
        }

        return getString(key, player.getLanguage());
    }

    public static String getString(String key, String localeString)
    {
        localeString = MyPetUtil.cutString(localeString, 2);
        LocaleUtils.toLocale(localeString);

        if (latestMyPetLocales == null)
        {
            return key;
        }
        return latestMyPetLocales.getText(key, localeString);
    }

    public String getText(String key, String localeString)
    {
        localeString = MyPetUtil.cutString(localeString, 2).toLowerCase();

        if (!locales.containsKey(localeString))
        {
            loadLocale(localeString);
        }

        ResourceBundle locale = locales.get(localeString);
        if (locale.containsKey(key))
        {
            return Colorizer.setColors(locale.getString(key));
        }

        locale = locales.get("en");
        if (locale.containsKey(key))
        {
            return Colorizer.setColors(locale.getString(key));
        }

        return key;
    }

    public void loadLocale(String localeString)
    {
        MyPetResourceBundle newLocale = null;
        if (jarFile != null)
        {
            try
            {
                JarEntry jarEntry = jarFile.getJarEntry("locale/MyPet_" + localeString + ".properties");
                if (jarEntry != null)
                {
                    ResourceBundle defaultBundle = new PropertyResourceBundle(new InputStreamReader(jarFile.getInputStream(jarEntry), "UTF-8"));
                    newLocale = new MyPetResourceBundle(defaultBundle);
                }
                else
                {
                    throw new IOException();
                }
            }
            catch (UnsupportedEncodingException e)
            {
                e.printStackTrace();
            }
            catch (IOException ignored)
            {
            }
        }
        if (newLocale == null)
        {
            newLocale = new MyPetResourceBundle();
        }

        File localeFile = new File(MyPetPlugin.getPlugin().getDataFolder() + File.separator + "locale" + File.separator + "MyPet_" + localeString + ".properties");
        if (localeFile.exists())
        {
            try
            {
                ResourceBundle optionalBundle = new PropertyResourceBundle(new InputStreamReader(new FileInputStream(localeFile), "UTF-8"));
                newLocale.addExtensionBundle(optionalBundle);
            }
            catch (UnsupportedEncodingException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        locales.put(localeString, newLocale);
    }
}
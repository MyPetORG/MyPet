/*
 * Copyright (C) 2011-2012 Keyle
 *
 * This file is part of MyWolf
 *
 * MyWolf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyWolf is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyWolf. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyWolf.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class MyWolfConfiguration
{
    public File ConfigFile;
    public FileConfiguration Config;

    public MyWolfConfiguration(String Path)
    {
        ConfigFile = new File(Path);
        Config = new YamlConfiguration();
        try
        {
            Config.load(ConfigFile);
        }
        catch (Exception ignored) {}
    }
    
    public MyWolfConfiguration(File f)
    {
        ConfigFile = f;
        Config = new YamlConfiguration();
        try
        {
            Config.load(ConfigFile);
        }
        catch (Exception ignored){}
    }

    public boolean saveConfig()
    {
        try
        {
            Config.save(ConfigFile);
            return true;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }
    }
}

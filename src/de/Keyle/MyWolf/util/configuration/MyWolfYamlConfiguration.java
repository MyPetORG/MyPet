/*
 * Copyright (C) 2011-2012 Keyle
 *
 * This file is part of MyPet
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
 * along with MyPet. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyWolf.util.configuration;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class MyWolfYamlConfiguration
{
    public File ConfigFile;
    private FileConfiguration Config;

    public MyWolfYamlConfiguration(String Path)
    {
        this(new File(Path));
    }

    public MyWolfYamlConfiguration(File f)
    {
        ConfigFile = f;
        Config = new YamlConfiguration();
        try
        {
            Config.load(ConfigFile);
        }
        catch (Exception ignored)
        {
        }
    }

    public FileConfiguration getConfig()
    {
        return Config;
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

    public void clearConfig()
    {
        Config = new YamlConfiguration();
    }
}
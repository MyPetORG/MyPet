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

package de.Keyle.MyPet.util.configuration;

import de.Keyle.MyPet.util.logger.DebugLogger;
import de.Keyle.MyPet.util.logger.MyPetLogger;
import org.bukkit.ChatColor;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigurationSnakeYAML
{
    public File yamlFile;
    private Map<String, Object> config;
    Yaml yaml;

    public ConfigurationSnakeYAML(String path)
    {
        this(new File(path));
    }

    public ConfigurationSnakeYAML(File file)
    {
        yamlFile = file;
        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setDefaultFlowStyle(FlowStyle.BLOCK);
        yaml = new Yaml(options);
    }

    public Map<String, Object> getConfig()
    {
        if (config == null)
        {
            clearConfig();
        }
        return config;
    }

    @SuppressWarnings("unchecked")
    public boolean load()
    {
        try
        {
            config = (Map<String, Object>) yaml.load(new FileInputStream(yamlFile));
            return true;
        }
        catch (ClassCastException e)
        {
            MyPetLogger.write(ChatColor.RED + "Could not parse/load " + yamlFile.getName());
            DebugLogger.warning("Could not parse/load " + yamlFile.getName());
            return false;
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
            return false;
        }
    }

    public boolean save()
    {
        try
        {
            FileOutputStream os = new FileOutputStream(yamlFile);
            os.write(yaml.dump(config).getBytes());
            os.close();

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
        config = new LinkedHashMap<String, Object>();
    }
}
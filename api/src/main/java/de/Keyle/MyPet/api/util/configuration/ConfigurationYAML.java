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

package de.Keyle.MyPet.api.util.configuration;

import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigurationYAML {

    public File yamlFile;
    private FileConfiguration config;

    public ConfigurationYAML(String path) {
        this(new File(path));
    }

    public ConfigurationYAML(File file) {
        yamlFile = file;
        loadConfig();
        file.setWritable(true);
        file.setReadable(true);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public boolean saveConfig() {
        try {
            config.save(yamlFile);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean loadConfig() {
        clearConfig();
        try {
            config.load(yamlFile);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public void clearConfig() {
        config = new org.bukkit.configuration.file.YamlConfiguration();
    }
}
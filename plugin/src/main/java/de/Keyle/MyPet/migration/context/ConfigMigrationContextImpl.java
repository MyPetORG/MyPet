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

package de.Keyle.MyPet.migration.context;

import de.Keyle.MyPet.migration.ConfigMigrationContext;
import de.Keyle.MyPet.migration.MigrationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigMigrationContextImpl implements ConfigMigrationContext {
    private final File dataFolder;
    private final Map<String, YamlConfiguration> loadedConfigs = new HashMap<>();

    public ConfigMigrationContextImpl(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    @Override
    public YamlConfiguration getConfig(String filename) throws MigrationException {
        YamlConfiguration cached = loadedConfigs.get(filename);
        if (cached != null) {
            return cached;
        }
        File configFile = new File(dataFolder, filename);
        if (!configFile.exists()) {
            throw new MigrationException("Config file not found: " + filename);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        loadedConfigs.put(filename, config);
        return config;
    }

    @Override
    public void saveConfig(String filename) throws MigrationException {
        YamlConfiguration config = loadedConfigs.get(filename);
        if (config == null) {
            throw new MigrationException("Config not loaded: " + filename);
        }
        try {
            config.save(new File(dataFolder, filename));
        } catch (IOException e) {
            throw new MigrationException("Failed to save config: " + filename, e);
        }
    }

    @Override
    public File getDataFolder() {
        return dataFolder;
    }
}

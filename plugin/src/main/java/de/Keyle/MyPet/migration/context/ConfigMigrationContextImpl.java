package de.Keyle.MyPet.migration.context;

import de.Keyle.MyPet.api.migration.ConfigMigrationContext;
import de.Keyle.MyPet.api.migration.MigrationException;
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

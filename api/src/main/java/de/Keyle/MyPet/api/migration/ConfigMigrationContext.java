package de.Keyle.MyPet.api.migration;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public interface ConfigMigrationContext {
    YamlConfiguration getConfig(String filename) throws MigrationException;

    void saveConfig(String filename) throws MigrationException;

    File getDataFolder();
}

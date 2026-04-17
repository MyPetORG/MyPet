package de.Keyle.MyPet.api.migration;

public interface ConfigMigration {
    void migrate(ConfigMigrationContext context) throws MigrationException;
}

package de.Keyle.MyPet.migration;

public interface ConfigMigration {
    void migrate(ConfigMigrationContext context) throws MigrationException;
}

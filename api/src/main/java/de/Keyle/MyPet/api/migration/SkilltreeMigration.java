package de.Keyle.MyPet.api.migration;

public interface SkilltreeMigration {
    void migrate(SkilltreeMigrationContext context) throws MigrationException;
}

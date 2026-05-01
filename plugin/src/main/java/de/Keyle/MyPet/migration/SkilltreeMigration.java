package de.Keyle.MyPet.migration;

public interface SkilltreeMigration {
    void migrate(SkilltreeMigrationContext context) throws MigrationException;
}

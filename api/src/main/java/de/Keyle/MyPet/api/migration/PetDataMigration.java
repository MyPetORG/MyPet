package de.Keyle.MyPet.api.migration;

public interface PetDataMigration {
    void migrateSql(SqlMigrationContext context) throws MigrationException;
}

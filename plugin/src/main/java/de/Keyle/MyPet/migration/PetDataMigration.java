package de.Keyle.MyPet.migration;

public interface PetDataMigration {
    void migrateSql(SqlMigrationContext context) throws MigrationException;
}

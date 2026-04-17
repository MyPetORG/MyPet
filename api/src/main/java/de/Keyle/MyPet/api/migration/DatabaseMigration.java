package de.Keyle.MyPet.api.migration;

public interface DatabaseMigration {
    void migrateSql(SqlMigrationContext context) throws MigrationException;
}

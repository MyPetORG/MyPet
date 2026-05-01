package de.Keyle.MyPet.migration;

public interface DatabaseMigration {
    void migrateSql(SqlMigrationContext context) throws MigrationException;
}

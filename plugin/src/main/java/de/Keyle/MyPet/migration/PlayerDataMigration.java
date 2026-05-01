package de.Keyle.MyPet.migration;

public interface PlayerDataMigration {
    void migrateSql(SqlMigrationContext context) throws MigrationException;
}

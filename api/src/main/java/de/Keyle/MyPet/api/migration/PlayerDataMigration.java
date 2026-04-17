package de.Keyle.MyPet.api.migration;

public interface PlayerDataMigration {
    void migrateSql(SqlMigrationContext context) throws MigrationException;
}

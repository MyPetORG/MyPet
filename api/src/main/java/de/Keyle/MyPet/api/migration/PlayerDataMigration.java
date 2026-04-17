package de.Keyle.MyPet.api.migration;

public interface PlayerDataMigration {
    default void migrateSql(SqlMigrationContext context) throws MigrationException {
    }

    default void migrateMongo(MongoMigrationContext context) throws MigrationException {
    }
}

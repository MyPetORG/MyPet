package de.Keyle.MyPet.api.migration;

public interface PetDataMigration {
    default void migrateSql(SqlMigrationContext context) throws MigrationException {
    }

    default void migrateMongo(MongoMigrationContext context) throws MigrationException {
    }
}

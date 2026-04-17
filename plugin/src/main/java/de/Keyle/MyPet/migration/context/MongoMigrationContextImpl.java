package de.Keyle.MyPet.migration.context;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.Keyle.MyPet.api.migration.MongoMigrationContext;
import org.bson.Document;

public class MongoMigrationContextImpl implements MongoMigrationContext {
    private final MongoDatabase database;
    private final String collectionPrefix;

    public MongoMigrationContextImpl(MongoDatabase database, String collectionPrefix) {
        this.database = database;
        this.collectionPrefix = collectionPrefix;
    }

    @Override
    public MongoCollection<Document> getCollection(String name) {
        return database.getCollection(collectionPrefix + name);
    }

    @Override
    public MongoDatabase getDatabase() {
        return database;
    }

    public String getCollectionPrefix() {
        return collectionPrefix;
    }
}

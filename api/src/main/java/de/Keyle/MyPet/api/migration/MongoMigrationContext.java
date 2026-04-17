package de.Keyle.MyPet.api.migration;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public interface MongoMigrationContext {
    MongoCollection<Document> getCollection(String name);

    MongoDatabase getDatabase();
}

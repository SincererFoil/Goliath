package ch.mcserver.goliath.database.mongodb;

import ch.mcserver.goliath.Goliath;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.bson.Document;

public class MongoDBManager {
    private MongoClient mongoClient;
    private MongoDatabase mongoDatabase;

    public void connect() {
        try {
            String uri = Goliath.config.node("mongodb", "uri").getString();

            mongoClient = MongoClients.create(uri);
            mongoDatabase = mongoClient.getDatabase("GoliathCommand");
            Goliath.LOGGER.info("[Goliath] MongoDB connected");
        } catch (Exception exception) {
            Goliath.LOGGER.error("[Goliath] MongoDB connection Failed. Reason: " + exception.getMessage().toString() );
        }
    }
    public MongoDatabase getMongoDatabase() {
        if (mongoDatabase == null) {
            Goliath.LOGGER.warn("[Goliath] MongoDB is not connected!");
        }
        return mongoDatabase;
    }
    public MongoCollection<Document> getCollection(String name) {
        return getMongoDatabase().getCollection(name);
    }
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            Goliath.LOGGER.info("[Goliath] MongoDB connection closed");
        }
    }
}

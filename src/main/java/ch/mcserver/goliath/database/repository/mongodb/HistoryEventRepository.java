package ch.mcserver.goliath.database.repository.mongodb;

import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.UUID;

public class HistoryEventRepository {

    private final MongoCollection<Document> collection;

    public HistoryEventRepository(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    public void createEvent(UUID uuid, String type, String title, String server, String historyId) {
        Document document = new Document()
                .append("historyId", historyId)
                .append("uuid", uuid.toString())
                .append("type", type)
                .append("title", title)
                .append("server", server)
                .append("createdAt", System.currentTimeMillis());

        collection.insertOne(document);
    }
}
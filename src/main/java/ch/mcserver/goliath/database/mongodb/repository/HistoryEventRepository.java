package ch.mcserver.goliath.database.mongodb.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HistoryEventRepository {

    private final MongoCollection<Document> collection;

    public HistoryEventRepository(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    public void createEvent(UUID uuid, String type, String title, String server, String historyId) {

        if (collection.countDocuments(Filters.eq("uuid", uuid.toString())) > 119) {
            List<ObjectId> ids = collection.find(Filters.eq("uuid", uuid.toString()))
                    .sort(Sorts.ascending("createdAt"))
                    .limit(1)
                    .projection(Projections.include("_id"))
                    .into(new ArrayList<>())
                    .stream()
                    .map(d -> d.getObjectId("_id"))
                    .toList();

            collection.deleteMany(Filters.in("_id", ids));
        }
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
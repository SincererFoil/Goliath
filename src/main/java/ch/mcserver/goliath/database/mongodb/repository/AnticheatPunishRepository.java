package ch.mcserver.goliath.database.mongodb.repository;

import ch.mcserver.goliath.anticheat.AutopunishMessage;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AnticheatPunishRepository {

    private final MongoCollection<Document> collection;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final String proxyName;

    public AnticheatPunishRepository(MongoCollection<Document> collection, String proxyName) {
        this.collection = collection;
        this.proxyName = proxyName;
    }

    public void createPunishmentLog(AutopunishMessage message, String ping, UUID uuid) {

        Document document = new Document()
                .append("punishmentUuid", uuid.toString())
                .append("playerUuid", message.flagData().playerUuid().toString())
                .append("username", message.flagData().playerName())
                .append("ping", ping)
                .append("goliath", message.flagData().serverName())
                .append("proxy", proxyName)
                .append("check", message.flagData().checkName())
                .append("buffer", message.flagData().details())
                .append("violations", message.flagData().violations())
                .append("timestamp", message.flagData().timestamp());

        executor.execute(() -> {
            collection.insertOne(document);
        });

    }


}

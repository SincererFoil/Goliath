package ch.mcserver.goliath.history;

import ch.mcserver.goliath.database.mongodb.repository.HistoryEventRepository;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.UUID;

public class HistroyLogTypes {

    private final ProxyServer proxy;
    private final SnapshotRequestManager messenger;
    private final HistoryEventRepository repository;




    /**
     * Constructor to initialize Objects
     * @param proxy the proxy obejct
     * @param messenger the RequestMessenger object.
     * @param repository the HistoryRepository used to create a new History Log
     */
    public HistroyLogTypes(ProxyServer proxy, SnapshotRequestManager messenger, HistoryEventRepository repository) {
        this.proxy = proxy;
        this.messenger = messenger;
        this.repository = repository;
    }




    public void switchTarget(UUID playerUuid, RegisteredServer oldServer, RegisteredServer newServer) {
        String oldServerName = oldServer.getServerInfo().getName();
        String newServerName = newServer.getServerInfo().getName();

        UUID historyId = UUID.randomUUID();
        String historyTitle = "switchTarget: " + newServerName;

        repository.createEvent(
                playerUuid,
                "switchTarget",
                historyTitle,
                oldServerName,
                historyId.toString()
        );

        messenger.requestSnapshot(
                playerUuid,
                historyId.toString(),
                "switchTarget"
        );
    }



    /**
     * History Log Event Join Server
     * gets executed when a player is joining the server.
     *
     * @param playerUuid the player's uuid
     * @param server
     */
    public void JoinHistory(UUID playerUuid, RegisteredServer server) {
        String serverName = server.getServerInfo().getName();
        // Saves the name of the current server

        UUID historyId = UUID.randomUUID();
        // Generates a random UUID for the history logId

        String historyTitle = "Join-History";
        // Set's the title of the event for the Database.

        repository.createEvent(playerUuid, "Join", historyTitle, serverName, historyId.toString());
        // Creates a new Event for the Database

        messenger.requestSnapshot(playerUuid, historyId.toString(), "Join");
        // Calls the messenger to send a message to the server where the player is located
    }

    public void kickHistory(UUID playerUuid, RegisteredServer server, String reason) {
        UUID historyId = UUID.randomUUID();
        String historyTitle = "Server-Kick ( " + reason + " )";
        repository.createEvent(playerUuid, "Kick", historyTitle, server.getServerInfo().getName(), historyId.toString());
        messenger.requestSnapshot(playerUuid, historyId.toString(), "Kick");
    }
}

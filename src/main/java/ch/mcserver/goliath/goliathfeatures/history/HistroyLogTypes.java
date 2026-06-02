package ch.mcserver.goliath.goliathfeatures.history;

import ch.mcserver.goliath.Goliath;
import ch.mcserver.goliath.database.repository.mongodb.HistoryEventRepository;
import ch.mcserver.goliath.pluginMessanger.SnapshotRequestManager;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.Optional;
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




    /**
     * History Log Event switchTarget
     * gets executed when a player is switching to a new server.
     *
     * @param playerUuid Player's UniqueID
     * @param server
     */
    public void switchTarget(UUID playerUuid, RegisteredServer server) {
        String serverName = server.getServerInfo().getName();
        // Saves the name of the current server

        UUID historyId = UUID.randomUUID();
        // Generates a random UUID for the history log id

        String historyTitle = "switchTarget: " + serverName;
        // String builder to build the title for the History

        repository.createEvent(playerUuid, "switchTarget", historyTitle, serverName, historyId.toString());
        // Creates a new Event for the Database

        messenger.requestSnapshot(playerUuid, historyId.toString(), "switchTarget");
        // Calls the messenger to send a message to the server where the player is located
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

    public void DisconnectHistory(UUID playerUuid) {
        Optional<Player> optionalPlayer = proxy.getPlayer(playerUuid);
        // Sets the player to an optional player to check if the player is present.
        if (!optionalPlayer.isPresent()) {
            UUID historyId = UUID.randomUUID();

            repository.createEvent(
                    playerUuid,
                    "Disconnect",
                    "Disconnect-History",
                    "UNKNOWN",
                    historyId.toString()
            );
            return;
        }

        Player player = optionalPlayer.get();
        // Converts the optional player to a Player

        String server = "UNKNOWN";

        Optional<ServerConnection> optionalServer = player.getCurrentServer();
        // Converts the current server to a optionalServer to check if the server is present.

        if (optionalServer.isPresent()) {
            server = optionalServer.get().getServerInfo().getName();
        }

        UUID historyId = UUID.randomUUID();
        // Generates a random UUID for the history logId

        String historyTitle = "PlayerQuit DISCONNECTING FREE";
        // Set's the title of the event for the Database.

        repository.createEvent(playerUuid, "Disconnect", historyTitle, server, historyId.toString());
        // Creates a new Event for the Database
    }


    public void kickHistory(UUID playerUuid, RegisteredServer server, String reason) {
        UUID historyId = UUID.randomUUID();
        String historyTitle = "Server-Kick ( " + reason + " )";
        repository.createEvent(playerUuid, "Kick", historyTitle, server.getServerInfo().getName(), historyId.toString());
        messenger.requestSnapshot(playerUuid, historyId.toString(), "Kick");
    }
}

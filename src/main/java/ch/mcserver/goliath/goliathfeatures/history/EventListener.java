package ch.mcserver.goliath.goliathfeatures.history;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.Optional;

public class EventListener {

    private static final Logger log = LoggerFactory.getLogger(EventListener.class);
    private HistroyLogTypes logTypes;

    public EventListener(HistroyLogTypes logTypes) {
        this.logTypes = logTypes;
    }




    @Subscribe
    public void onServerSwitch(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        Optional<RegisteredServer> previousServer = event.getPreviousServer();
        // Converts the previousServer to an Optional RegisterdServer to check if the previous server exists,
        // This is the normal Method to check if the player switched the server or joined the server.
        if (previousServer.isEmpty()) {
            logTypes.JoinHistory(player.getUniqueId(), event.getServer());
            // Executes the Join History Type
        } else {
            logTypes.switchTarget(player.getUniqueId(), event.getServer());
            // Executes the switchTarget History Type.
        }
    }

    @Subscribe
    public void onPlayerDiconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        logTypes.DisconnectHistory(player.getUniqueId());
        // Execute the DisconnectHistory Log
    }
    @Subscribe
    public void onPlayer(KickedFromServerEvent event) {
        Player player = event.getPlayer();
        RegisteredServer server = event.getServer();
        String reason = event.getServerKickReason().toString();
        logTypes.kickHistory(player.getUniqueId(), server, reason);
    }
}
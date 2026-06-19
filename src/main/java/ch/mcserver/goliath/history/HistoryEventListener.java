package ch.mcserver.goliath.history;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class HistoryEventListener {

    private static final Logger log = LoggerFactory.getLogger(HistoryEventListener.class);
    private HistroyLogTypes logTypes;
    private final ProxyServer proxy;
    private final Object plugin;

    public HistoryEventListener(HistroyLogTypes logTypes, ProxyServer proxy, Object plugin) {
        this.logTypes = logTypes;
        this.proxy = proxy;
        this.plugin = plugin;
    }




    @Subscribe
    public void onServerSwitch(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        Optional<RegisteredServer> previousServer = event.getPreviousServer();

        if (previousServer.isEmpty()) {
            proxy.getScheduler()
                    .buildTask(plugin, () -> logTypes.JoinHistory(player.getUniqueId(), event.getServer()))
                    .delay(1, TimeUnit.SECONDS)
                    .schedule();
        } else {
            proxy.getScheduler()
                    .buildTask(plugin, () -> logTypes.switchTarget(player.getUniqueId(), event.getServer()))
                    .delay(1, TimeUnit.SECONDS)
                    .schedule();
        }
    }

    @Subscribe
    public void onPlayerDiconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        logTypes.DisconnectHistory(player.getUniqueId());
    }
    @Subscribe
    public void onPlayer(KickedFromServerEvent event) {
        Player player = event.getPlayer();
        RegisteredServer server = event.getServer();
        String reason = "Server Kick";
        logTypes.kickHistory(player.getUniqueId(), server, reason);
    }
}
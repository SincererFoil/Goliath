package ch.mcserver.goliath.history;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
        RegisteredServer newServer = event.getServer();
        Optional<RegisteredServer> previousServer = event.getPreviousServer();

        if (previousServer.isEmpty()) {
            proxy.getScheduler()
                    .buildTask(plugin, () -> logTypes.JoinHistory(player.getUniqueId(), newServer))
                    .delay(1, TimeUnit.SECONDS)
                    .schedule();
            return;
        }

        RegisteredServer oldServer = previousServer.get();

        proxy.getScheduler()
                .buildTask(plugin, () -> logTypes.switchTarget(
                        player.getUniqueId(),
                        oldServer,
                        newServer
                ))
                .delay(1, TimeUnit.SECONDS)
                .schedule();
    }

    @Subscribe
    public void onPlayerKick(KickedFromServerEvent event) {
        Player player = event.getPlayer();
        RegisteredServer server = event.getServer();

        String reason = event.getServerKickReason()
                .map(component -> PlainTextComponentSerializer.plainText().serialize(component))
                .filter(text -> !text.isBlank())
                .orElse("Unknown reason");

        logTypes.kickHistory(player.getUniqueId(), server, reason);
    }
}
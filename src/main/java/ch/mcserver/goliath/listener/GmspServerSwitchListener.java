package ch.mcserver.goliath.listener;

import ch.mcserver.goliath.player.ProxyPlayerObject;
import ch.mcserver.goliath.pluginmessenger.GmspMessenger;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import java.util.concurrent.TimeUnit;

import static ch.mcserver.goliath.player.ProxyPlayerManager.getPlayer;

public class GmspServerSwitchListener {

    private final ProxyServer proxy;
    private final Object plugin;
    private final GmspMessenger messenger;

    public GmspServerSwitchListener(ProxyServer proxy, Object plugin, GmspMessenger messenger) {
        this.proxy = proxy;
        this.plugin = plugin;
        this.messenger = messenger;
    }

    @Subscribe
    public void onServerSwitch(ServerConnectedEvent event) {

        Player player = event.getPlayer();

        ProxyPlayerObject proxyPlayerObject =
                getPlayer(player.getUniqueId());

        if (proxyPlayerObject == null) {
            return;
        }

        if (!proxyPlayerObject.isGmsp()) {
            return;
        }

        proxy.getScheduler()
                .buildTask(plugin, () -> messenger.sendGmsp(player, true))
                .delay(500, TimeUnit.MILLISECONDS)
                .schedule();
    }
}

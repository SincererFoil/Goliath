package ch.mcserver.goliath.listener;

import ch.mcserver.goliath.player.ProxyPlayerObject;
import ch.mcserver.goliath.pluginmessenger.CreativeMessenger;
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
    private final GmspMessenger gmspMessenger;
    private final CreativeMessenger creativeMessenger;

    public GmspServerSwitchListener(ProxyServer proxy, Object plugin, GmspMessenger gmspMessenger, CreativeMessenger creativeMessenger) {
        this.proxy = proxy;
        this.plugin = plugin;
        this.gmspMessenger = gmspMessenger;
        this.creativeMessenger = creativeMessenger;
    }

    @Subscribe
    public void onServerSwitch(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        var destination = event.getServer();

        proxy.getScheduler()
                .buildTask(plugin, () -> {
                    if (player.getCurrentServer().isEmpty()
                            || !player.getCurrentServer().get()
                            .getServer().equals(destination)) {
                        return;
                    }

                    ProxyPlayerObject playerObject =
                            getPlayer(player.getUniqueId());

                    if (playerObject == null) {
                        return;
                    }

                    gmspMessenger.sendGmsp(
                            player,
                            playerObject.isGmsp()
                    );

                    if (playerObject.isCreative()) {
                        creativeMessenger.sendCreative(player, true);
                    }
                })
                .delay(500, TimeUnit.MILLISECONDS)
                .schedule();
    }

}

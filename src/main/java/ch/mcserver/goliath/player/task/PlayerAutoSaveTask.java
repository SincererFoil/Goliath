package ch.mcserver.goliath.player.task;

import ch.mcserver.goliath.player.ProxyPlayerManager;
import ch.mcserver.goliath.player.ProxyPlayerObject;
import com.google.common.eventbus.Subscribe;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import static ch.mcserver.goliath.Goliath.playerRepository;

public class PlayerAutoSaveTask {

    private final ProxyServer proxy;

    public PlayerAutoSaveTask(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Subscribe
    public void autoScheduler() {

        proxy.getScheduler()
                .buildTask(this, () -> {
                    for (Player player : proxy.getAllPlayers()) {
                        ProxyPlayerObject playerObject = ProxyPlayerManager.getPlayer(player.getUniqueId());
                        if (playerObject == null) {
                            continue;
                        }
                        try {
                            playerRepository.savePlayerDataOnly(playerObject);
                        } catch (Exception exception) {
                            exception.printStackTrace();
                            return;
                        }


                    }
                })
                .repeat(2, java.util.concurrent.TimeUnit.MINUTES)
                .schedule();
    }
}

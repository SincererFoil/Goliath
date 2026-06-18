package ch.mcserver.goliath.service.joinservice;

import ch.mcserver.goliath.database.mysql.repository.PlayerRepository;
import ch.mcserver.goliath.player.ProxyPlayerObject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;

public class JoinController {

    private final PlayerRepository playerRepository;

    public JoinController(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();

        String currentServer = event.getServer()
                .getServerInfo()
                .getName();

        ProxyPlayerObject playerObject = playerRepository.loadPlayer(player.getUniqueId());

        if (playerObject == null) {
            return;
        }
        playerObject.setCurrentServer(currentServer);
        playerRepository.save(playerObject);

    }
}
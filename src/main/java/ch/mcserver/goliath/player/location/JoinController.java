package ch.mcserver.goliath.player.location;

import ch.mcserver.goliath.database.mysql.repository.PlayerLocationObject;
import ch.mcserver.goliath.database.mysql.repository.PlayerLocationRepository;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Optional;

public class JoinController {
    private ProxyServer proxy;
    private PlayerLocationRepository playerLocationRepository;
    public  JoinController(ProxyServer proxy, PlayerLocationRepository playerLocationRepository) {
        this.proxy = proxy;
        this.playerLocationRepository = playerLocationRepository;
    }
    @Subscribe
    public void onConnect(ServerPreConnectEvent event) {
        Player player = event.getPlayer();
        PlayerLocationObject location = playerLocationRepository.loadPlayer(player.getUniqueId());

        if (location == null) {
            return;
        }
        String serverName = location.getServerName();
        if (serverName == null || serverName.isBlank()) {
            return;
        }
        if (location.getServerName().toLowerCase().contains("spawn")) return;

        Optional<RegisteredServer> optionalServer = proxy.getServer(location.getServerName());
        if (optionalServer.isEmpty()) {
            player.disconnect(Component.text("You should make a ticket.", NamedTextColor.RED).append(Component.text("Error: #G404NF", NamedTextColor.RED)));
            return;
        }
        if (player.getCurrentServer().isPresent()) {
            return;
        }
        event.setResult(ServerPreConnectEvent.ServerResult.allowed(optionalServer.get()));

    }
}
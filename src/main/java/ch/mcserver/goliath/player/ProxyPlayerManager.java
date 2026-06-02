package ch.mcserver.goliath.player;

import ch.mcserver.goliath.Goliath;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProxyPlayerManager {

    private static final Map<UUID, ProxyPlayerObject> players = new HashMap<>();

    public static void addPlayer(ProxyPlayerObject playerObject) {
        players.put(playerObject.getUuid(), playerObject);
    }

    public static ProxyPlayerObject getPlayer(UUID uuid) {
        return players.get(uuid);
    }

    public static boolean hasPlayer(UUID uuid) {
        return players.containsKey(uuid);
    }

    public static void removePlayer(UUID uuid) {
        players.remove(uuid);
    }

    @Subscribe
    public void onPlayerConnect(PostLoginEvent event) {

        Player player = event.getPlayer();

        UUID uuid = player.getUniqueId();

        ProxyPlayerObject playerObject;

        if (Goliath.playerRepository.exists(uuid)) {

            playerObject = Goliath.playerRepository.loadPlayer(uuid);
            playerObject.setSfmode(false);
            playerObject.setGmsp(false);


        } else {

            long now = System.currentTimeMillis();

            playerObject = new ProxyPlayerObject(
                    uuid,
                    player.getUsername(),
                    "Player",
                    "none",
                    false,
                    false,
                    false,
                    false,
                    0.1f,
                    now,
                    now,
                    null
            );

            Goliath.playerRepository.create(playerObject);
        }

        addPlayer(playerObject);
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {

        Player player = event.getPlayer();

        ProxyPlayerObject playerObject = getPlayer(player.getUniqueId());

        if (playerObject == null) {
            return;
        }
        playerObject.setSfmode(false);
        playerObject.setGmsp(false);

        Goliath.playerRepository.savePlayerDataOnly(playerObject);

        removePlayer(player.getUniqueId());
    }
}

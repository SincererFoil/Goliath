package ch.mcserver.goliath.player;

import ch.mcserver.goliath.Goliath;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyPlayerManager {

    private static final Map<UUID, ProxyPlayerObject> players = new ConcurrentHashMap<>();

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
    public EventTask onPlayerConnect(PostLoginEvent event) {
        return EventTask.async(() -> {

            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();

            try {
                ProxyPlayerObject playerObject;

                if (Goliath.playerRepository.exists(uuid)) {

                    playerObject = Goliath.playerRepository.loadPlayer(uuid);
                    playerObject.setSfmode(false);

                    if (!playerObject.getName().equals(player.getUsername())) {
                        playerObject.setName(player.getUsername());
                        Goliath.playerRepository.save(playerObject);
                    }

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
                            0.1f,
                            now,
                            now,
                            false,
                            new ArrayList<>()
                    );

                    Goliath.playerRepository.create(playerObject);
                }

                addPlayer(playerObject);

            } catch (Exception e) {
                Goliath.LOGGER.error("Failed to load player data for {}", uuid, e);
                player.disconnect(Component.text("We dont know what happend here! You should make a ticket  #DBLF054", NamedTextColor.RED));
            }
        });
    }

    @Subscribe
    public EventTask onPlayerDisconnect(DisconnectEvent event) {
        return EventTask.async(() -> {

            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();
            ProxyPlayerObject playerObject = getPlayer(uuid);

            if (playerObject == null) {
                return;
            }

            playerObject.setSfmode(false);
            playerObject.setGmsp(false);

            try {
                Goliath.playerRepository.savePlayerDataOnly(playerObject);
            } catch (Exception e) {
                Goliath.LOGGER.error("Failed to save player data for {}", uuid, e);
            } finally {
                removePlayer(uuid);
            }
        });
    }
}
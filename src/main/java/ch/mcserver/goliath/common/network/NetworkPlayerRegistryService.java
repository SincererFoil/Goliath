package ch.mcserver.goliath.common.network;

import ch.mcserver.goliath.Goliath;
import ch.mcserver.goliath.database.redis.RedisManager;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import redis.clients.jedis.Jedis;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static ch.mcserver.goliath.Goliath.proxyName;

public class NetworkPlayerRegistryService {

    private final ProxyServer proxy;

    private static final String PLAYER_KEY_PREFIX = "goliath:network:player:";

    private static final String PLAYER_KEY = PLAYER_KEY_PREFIX + proxyName;


    private final Goliath plugin;

    private final RedisManager redisManager;

    private ScheduledTask task;

    private volatile Set<String> onlinePlayerNames = Set.of();


    public NetworkPlayerRegistryService(ProxyServer proxy, Goliath  plugin, RedisManager redisManager) {
        this.proxy = proxy;
        this.plugin = plugin;
        this.redisManager = redisManager;
    }

    public void start() {
        task = proxy.getScheduler().buildTask(plugin, this::updateRegistry)
                .repeat(5, TimeUnit.SECONDS).schedule();
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }

        try (Jedis jedis = redisManager.getConnection()) {
            jedis.del(PLAYER_KEY);
        } catch (Exception e) {
            Goliath.LOGGER.error("Failed to remove player registry", e);
        }
    }

    private void updateRegistry() {
        try (Jedis jedis = redisManager.getConnection()) {

            Set<String> localPlayers = proxy.getAllPlayers()
                    .stream()
                    .map(Player::getUsername)
                    .collect(Collectors.toSet());

            jedis.del(PLAYER_KEY);

            if (!localPlayers.isEmpty()) {
                jedis.sadd(PLAYER_KEY, localPlayers.toArray(new String[0]));
                jedis.expire(PLAYER_KEY, 15);
            }

            Set<String> globalPlayers = new HashSet<>();

            for (String key : jedis.keys(PLAYER_KEY_PREFIX + "*")) {
                globalPlayers.addAll(jedis.smembers(key));
            }

            onlinePlayerNames = Set.copyOf(globalPlayers);

        } catch (Exception e) {
            Goliath.LOGGER.error("Failed to update player registry", e);
        }
    }

    public Set<String> getOnlinePlayerNames() {
        return onlinePlayerNames;
    }

}

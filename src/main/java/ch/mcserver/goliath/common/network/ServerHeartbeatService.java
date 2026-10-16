package ch.mcserver.goliath.common.network;


import ch.mcserver.goliath.Goliath;
import ch.mcserver.goliath.database.redis.RedisManager;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import redis.clients.jedis.Jedis;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static ch.mcserver.goliath.Goliath.proxyName;

public class ServerHeartbeatService {

    private final ProxyServer proxy;

    private static final String HEARTBEAT_KEY_PREFIX = "goliath:network:heartbeat:proxy:";
    private final Goliath  plugin;

    private final String instanceId = UUID.randomUUID().toString().substring(0, 8);

    private final RedisManager redisManager;

    public ServerHeartbeatService(ProxyServer proxy, Goliath  plugin, RedisManager redisManager) {
        this.proxy = proxy;
        this.plugin = plugin;
        this.redisManager = redisManager;
    }

    public void start() {
        proxy.getScheduler().buildTask(plugin, this::sendHeartbeat)
                .repeat(5, TimeUnit.SECONDS).schedule();
    }

    public void sendHeartbeat() {

        try (Jedis jedis = redisManager.getConnection()) {
            jedis.hset(HEARTBEAT_KEY_PREFIX + proxyName, Map.of(
                    "instance_id", instanceId,
                    "players", String.valueOf(proxy.getPlayerCount()),
                    "status", "ONLINE",
                    "timestamp", String.valueOf(System.currentTimeMillis())
            ));

            jedis.expire(HEARTBEAT_KEY_PREFIX + proxyName, 15);
        } catch (Exception exception) {

            Goliath.LOGGER.error("Failed to send proxy heartbeat", exception);
        }
    }
}

package ch.mcserver.goliath.rtp;

import ch.mcserver.goliath.database.redis.RedisManager;
import com.google.gson.Gson;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RandomTeleportService {

    private static final String CHANNEL = "goliath:rtp:request";

    private final RedisManager redisManager;
    private final ProxyServer proxy;
    private final ExecutorService subscriberExecutor = Executors.newSingleThreadExecutor();

    private volatile boolean running;
    private volatile Jedis activeConnection;
    private volatile JedisPubSub activeSubscriber;

    private final Gson gson = new Gson();

    public RandomTeleportService(ProxyServer proxy, RedisManager redisManager) {
        this.proxy = proxy;
        this.redisManager = redisManager;
    }

    public synchronized void start() {
        if (running) return;

        running = true;
        subscriberExecutor.submit(this::subscribeLoop);
    }

    public synchronized void stop() {
        running = false;

        JedisPubSub subscriber = activeSubscriber;
        if (subscriber != null) {
            subscriber.unsubscribe();
        }

        Jedis connection = activeConnection;
        if (connection != null) {
            connection.close();
        }

        subscriberExecutor.shutdownNow();
    }

    private void subscribeLoop() {
        while (running) {
            try (Jedis jedis = redisManager.getConnection()) {
                activeConnection = jedis;

                JedisPubSub subscriber = new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        if (!running || !CHANNEL.equals(channel) || message == null) return;

                        try {
                            handleRandomTeleportRequest(message);
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    }
                };

                activeSubscriber = subscriber;
                jedis.subscribe(subscriber, CHANNEL);
            } catch (Exception exception) {
                if (running) {
                    exception.printStackTrace();
                }
            } finally {
                activeSubscriber = null;
                activeConnection = null;
            }

            if (!running) break;

            try {
                Thread.sleep(3000);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void handleRandomTeleportRequest(String json) {
        try {
            if (json == null || json.isBlank()) return;

            RandomTeleportRequestData data = gson.fromJson(json, RandomTeleportRequestData.class);

            if (data == null || data.requestUuid() == null || data.playerUuid() == null || data.sourceServer() == null || data.sourceServer().isBlank() || data.region() == null || data.dimension() == null) {
                return;
            }

            Optional<Player> optionalPlayer = proxy.getPlayer(data.playerUuid());
            if (optionalPlayer.isEmpty()) return;

            Player player = optionalPlayer.get();
            if (player.getCurrentServer().isEmpty()) return;

            String currentServer = player.getCurrentServer().get().getServerInfo().getName();
            if (!currentServer.equals(data.sourceServer())) return;



        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public void findMatchingServer(RandomTeleportRequestData data) {
        Map<String, ServerData> servers = new HashMap<>();
        String prefix = "goliath:network:heartbeat:paper:";
        List<ServerData> possibleServers = new ArrayList<>();

        try (Jedis jedis = redisManager.getConnection()) {
            String cursor = ScanParams.SCAN_POINTER_START;
            ScanParams params = new ScanParams().match(prefix + "*").count(100);

            do {
                ScanResult<String> result = jedis.scan(cursor, params);

                for (String key : result.getResult()) {
                    Map<String, String> fields = jedis.hgetAll(key);
                    if (fields.isEmpty()) continue;

                    try {
                        String serverName = key.substring(prefix.length());

                        ServerData server = new ServerData(
                                serverName,
                                fields.get("instance_id"),
                                Integer.parseInt(fields.get("players")),
                                fields.get("status"),
                                Double.parseDouble(fields.get("tps")),
                                Double.parseDouble(fields.get("mspt")),
                                Long.parseLong(fields.get("timestamp"))
                        );

                        servers.put(serverName, server);
                    } catch (NumberFormatException | NullPointerException exception) {
                        exception.printStackTrace();
                    }
                }

                cursor = result.getCursor();
            } while (!ScanParams.SCAN_POINTER_START.equals(cursor));
        }
        for (ServerData server : servers.values()) {

        }
    }
}
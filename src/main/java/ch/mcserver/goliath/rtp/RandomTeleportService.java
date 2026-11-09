
package ch.mcserver.goliath.rtp;

import ch.mcserver.goliath.database.redis.RedisManager;
import com.google.gson.Gson;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RandomTeleportService {

    private static final String REQUEST_CHANNEL = "goliath:rtp:request";
    private static final String RESPONSE_CHANNEL = "goliath:rtp:response";
    private static final String ERROR_CHANNEL = "goliath:rtp:error";
    private static final String TRANSFER_CHANNEL = "goliath:rtp:transfer";

    private static final String HEARTBEAT_PREFIX = "goliath:network:heartbeat:paper:";
    private static final long HEARTBEAT_TIMEOUT = 15000L;

    private final RedisManager redisManager;
    private final ProxyServer proxy;
    private final Gson gson = new Gson();

    private final ExecutorService subscriberExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService requestExecutor = Executors.newSingleThreadExecutor();

    private volatile boolean running;
    private volatile Jedis activeConnection;
    private volatile JedisPubSub activeSubscriber;

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
        if (!running) return;

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
        requestExecutor.shutdownNow();
    }

    private void subscribeLoop() {
        while (running) {
            try (Jedis jedis = redisManager.getConnection()) {
                activeConnection = jedis;

                JedisPubSub subscriber = new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        if (!running || message == null || message.isBlank()) return;

                        try {
                            switch (channel) {
                                case REQUEST_CHANNEL -> requestExecutor.submit(() -> handleRandomTeleportRequest(message));
                                case RESPONSE_CHANNEL -> handleRandomTeleportResponse(message);
                                default -> {
                                }
                            }
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    }
                };

                activeSubscriber = subscriber;
                jedis.subscribe(subscriber, REQUEST_CHANNEL, RESPONSE_CHANNEL);

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

            findMatchingServer(data);

        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public void findMatchingServer(RandomTeleportRequestData data) {
        List<ServerData> possibleServers = new ArrayList<>();

        try (Jedis jedis = redisManager.getConnection()) {
            String cursor = ScanParams.SCAN_POINTER_START;
            ScanParams params = new ScanParams()
                    .match(HEARTBEAT_PREFIX + "*")
                    .count(100);

            do {
                ScanResult<String> result = jedis.scan(cursor, params);

                for (String key : result.getResult()) {
                    Map<String, String> fields = jedis.hgetAll(key);
                    if (fields.isEmpty()) continue;

                    try {
                        String serverName = key.substring(HEARTBEAT_PREFIX.length());

                        ServerData server = new ServerData(
                                serverName,
                                fields.get("instance_id"),
                                fields.get("region"),
                                fields.get("dimension"),
                                Integer.parseInt(fields.get("players")),
                                fields.get("status"),
                                Double.parseDouble(fields.get("tps")),
                                Double.parseDouble(fields.get("mspt")),
                                Long.parseLong(fields.get("timestamp"))
                        );

                        if (!Objects.equals(server.region(), data.region().name())) continue;
                        if (!Objects.equals(server.dimension(), data.dimension())) continue;
                        if (!"ONLINE".equals(server.status())) continue;
                        if (server.serverName() == null || server.serverName().isBlank()) continue;
                        if (server.timestamp() > System.currentTimeMillis()) continue;
                        if (System.currentTimeMillis() - server.timestamp() > HEARTBEAT_TIMEOUT) continue;
                        if (!Double.isFinite(server.tps()) || !Double.isFinite(server.mspt())) continue;
                        if (server.tps() < 0 || server.mspt() < 0) continue;
                        if (server.players() < 0) continue;

                        possibleServers.add(server);

                    } catch (NumberFormatException | NullPointerException exception) {
                        exception.printStackTrace();
                    }
                }

                cursor = result.getCursor();

            } while (!ScanParams.SCAN_POINTER_START.equals(cursor));

        } catch (Exception exception) {
            exception.printStackTrace();
            publishError(data);
            return;
        }

        ServerData serverData = possibleServers.stream().max(Comparator.comparingDouble(ServerData::tps).thenComparing(Comparator.comparingDouble(ServerData::mspt).reversed()))
                .orElse(null);

        if (serverData == null) {
            publishError(data);
            return;
        }

        RandomTeleportTransferData transferData = new RandomTeleportTransferData(
                data,
                serverData.serverName()
        );

        try (Jedis jedis = redisManager.getConnection()) {
            jedis.publish(TRANSFER_CHANNEL, gson.toJson(transferData));
        } catch (Exception exception) {
            exception.printStackTrace();
            publishError(data);
        }
    }

    private void handleRandomTeleportResponse(String json) {
        try {
            if (json == null || json.isBlank()) return;

            RandomTeleportTransferData transferData = gson.fromJson(
                    json,
                    RandomTeleportTransferData.class
            );

            if (transferData == null
                    || transferData.requestData() == null
                    || transferData.requestData().requestUuid() == null
                    || transferData.requestData().playerUuid() == null
                    || transferData.requestData().sourceServer() == null
                    || transferData.server() == null
                    || transferData.server().isBlank()) {
                return;
            }

            RandomTeleportRequestData data = transferData.requestData();

            Optional<Player> optionalPlayer = proxy.getPlayer(data.playerUuid());
            if (optionalPlayer.isEmpty()) return;

            Player player = optionalPlayer.get();
            if (player.getCurrentServer().isEmpty()) return;

            String currentServer = player.getCurrentServer().get().getServerInfo().getName();

            if (!currentServer.equals(data.sourceServer())) return;

            Optional<RegisteredServer> optionalServer = proxy.getServer(transferData.server());

            if (optionalServer.isEmpty()) {
                publishError(data);
                return;
            }

            RegisteredServer server = optionalServer.get();

            player.createConnectionRequest(server)
                    .connectWithIndication()
                    .whenComplete((success, throwable) -> {
                        if (throwable != null || !Boolean.TRUE.equals(success)) {
                            if (throwable != null) {
                                throwable.printStackTrace();
                            }

                            publishError(data);
                        }
                    });

        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void publishError(RandomTeleportRequestData data) {
        try (Jedis jedis = redisManager.getConnection()) {
            jedis.publish(ERROR_CHANNEL, gson.toJson(data));
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}

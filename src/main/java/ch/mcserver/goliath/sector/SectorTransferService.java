package ch.mcserver.goliath.sector;

import ch.mcserver.goliath.command.moderation.punish.PunishEvent;
import ch.mcserver.goliath.database.redis.RedisManager;
import com.google.gson.Gson;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SectorTransferService {

    private static final String CHANNEL = "goliath:sector:transfer:request";

    private final RedisManager redisManager;

    private final ProxyServer proxy;

    private Gson gson = new Gson();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final ExecutorService transferExecutor = Executors.newFixedThreadPool(2);

    private volatile boolean running = false;

    private volatile Jedis activeConnection;

    private volatile JedisPubSub activeSubscriber;

    public SectorTransferService(ProxyServer proxy, RedisManager redisManager) {
        this.redisManager = redisManager;
        this.proxy = proxy;
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        executor.submit(this::subscribeLoop);
    }

    public void stop() {
        running = false;

        JedisPubSub subscriber = activeSubscriber;
        if (subscriber != null) {
            subscriber.unsubscribe();
        }

        Jedis jedis = activeConnection;

        if (jedis != null) {
            jedis.close();
        }

        transferExecutor.shutdownNow();

        executor.shutdownNow();
    }

    private void subscribeLoop() {
        while (running) {

            try (Jedis jedis = redisManager.getConnection()) {

                activeConnection = jedis;

                JedisPubSub subscriber = new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        try {
                            handleTransferEvent(message);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };

                activeSubscriber = subscriber;

                jedis.subscribe(subscriber, CHANNEL);


            } catch (Exception e) {
                if (running) {
                    e.printStackTrace();
                }
            } finally {
                activeConnection = null;
                activeSubscriber = null;
            }


            if (!running) break;

            try {
                Thread.sleep(3000);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            }

        }
    };

    private void handleTransferEvent(String json) {

        try {
            SectorTransferData data = gson.fromJson(json, SectorTransferData.class);

            if (data == null || data.transferUuid() == null || data.playerUuid() == null || data.sourceServer() == null || data.targetServer() == null || data.dimension() == null) return;

            if (data.sourceServer().isBlank() || data.targetServer().isBlank() || data.dimension().isBlank() || !Double.isFinite(data.x()) || !Double.isFinite(data.y()) || !Double.isFinite(data.z()) || !Float.isFinite(data.yaw()) || !Float.isFinite(data.pitch())) return;

            Optional<Player> optionalPlayer = proxy.getPlayer(data.playerUuid());
            if (optionalPlayer.isEmpty()) return;

            Player player = optionalPlayer.get();

            if (player.getCurrentServer().isEmpty()) return;

            String currentServer = player.getCurrentServer().get().getServerInfo().getName();

            if (!currentServer.equals(data.sourceServer())) return;


            Optional<RegisteredServer> optionalTarget = proxy.getServer(data.targetServer());

            if (optionalTarget.isEmpty()) return;

            RegisteredServer targetServer = optionalTarget.get();

            executeTransfer(player, targetServer, data);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void executeTransfer(Player player, RegisteredServer targetServer, SectorTransferData data) {
        transferExecutor.execute(() -> {
            try (Jedis jedis = redisManager.getConnection()) {
                jedis.setex("goliath:sector:transfer:pending:" + data.playerUuid(), 30, gson.toJson(data));

                player.createConnectionRequest(targetServer).connect().whenComplete((result, throwable) -> {
                            if (throwable != null || !result.isSuccessful()) {
                                try (Jedis cleanup = redisManager.getConnection()) {
                                    cleanup.del("goliath:sector:transfer:pending:" + data.playerUuid());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }



}

package ch.mcserver.goliath.anticheat;

import ch.mcserver.goliath.Goliath;
import ch.mcserver.goliath.anticheat.alert.AnticheatAlert;
import ch.mcserver.goliath.database.redis.RedisManager;
import com.google.gson.Gson;
import com.mysql.cj.protocol.x.XProtocolRowInputStream;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.Jedis;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AnticheatFlagSubscriber {

    private final Gson gson = new  Gson();

    private final RedisManager redisManager;

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    private volatile  JedisPubSub flagSubscriber;

    private volatile  JedisPubSub autopunishSubscriber;

    private static final String FLAG_CHANNEL = "goliath:anticheat:flag";

    private static final String AUTOPUNISH_CHANNEL = "goliath:anticheat:autopunish";

    private final ProxyServer proxy;


    public  AnticheatFlagSubscriber(RedisManager redisManager, ProxyServer proxy) {
        this.redisManager = redisManager;
        this.proxy = proxy;
    }

    public void start() {
        executorService.execute(() -> {
            try(Jedis jedis = redisManager.getConnection()) {
                flagSubscriber = new JedisPubSub() {
                    @Override
                    public  void onMessage(String channel, String message) {
                        AnticheatFlagMessage flagMessage = gson.fromJson(message, AnticheatFlagMessage.class);

                        // TODO ALERT + 1x  /sus entry
                        sendAlert(flagMessage);
                    }
                };

                jedis.subscribe(flagSubscriber, FLAG_CHANNEL);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        });

        executorService.execute(() -> {
            try(Jedis jedis = redisManager.getConnection()) {
                autopunishSubscriber = new JedisPubSub() {
                    @Override
                    public  void onMessage(String channel, String message) {
                        AutopunishMessage flagMessage = gson.fromJson(message, AutopunishMessage.class);

                        Optional<Player> optionalPlayer = proxy.getPlayer(flagMessage.flagData().playerName());

                        String ping = "?";


                        if (optionalPlayer.isPresent()) {
                            ping = optionalPlayer.get().getPing() + "ms";
                        }
                        Goliath.LOGGER.info("BANNED PLAYER " + flagMessage.flagData().playerName());
                        proxy.getCommandManager().executeAsync(proxy.getConsoleCommandSource(), "offend " + flagMessage.flagData().playerName() + " " + flagMessage.punishReason() + " note:" + flagMessage.flagData().checkName() + " | VL=" +
                                flagMessage.flagData().violations() + " | BUFFER=" + flagMessage.flagData().details() + " | PING=" + ping);

                        Goliath.LOGGER.info("BANNED PLAYER ");
                    }
                };

                jedis.subscribe(autopunishSubscriber, AUTOPUNISH_CHANNEL);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private void sendAlert(AnticheatFlagMessage flagMessage) {
        Goliath.getInstance().getAnticheatAlert().sendAlert(flagMessage);
    }

    public void shutdown() {
        if (flagSubscriber != null && flagSubscriber.isSubscribed()) {
            flagSubscriber.unsubscribe();
        }
        if (autopunishSubscriber != null && autopunishSubscriber.isSubscribed()) {
            autopunishSubscriber.unsubscribe();
        }
        executorService.shutdown();
        try {
            boolean finished = executorService.awaitTermination(5, TimeUnit.SECONDS);

            if (!finished) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException exception) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}

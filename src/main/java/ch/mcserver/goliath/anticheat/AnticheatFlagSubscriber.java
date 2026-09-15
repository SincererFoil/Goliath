package ch.mcserver.goliath.anticheat;

import ch.mcserver.goliath.Goliath;
import ch.mcserver.goliath.anticheat.alert.AnticheatAlert;
import ch.mcserver.goliath.database.redis.RedisManager;
import com.google.gson.Gson;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.Jedis;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AnticheatFlagSubscriber {

    private final Gson gson = new  Gson();

    private final RedisManager redisManager;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private volatile  JedisPubSub jedisPubSub;

    private static final String CHANNEL = "goliath:anticheat:flag";


    public  AnticheatFlagSubscriber(RedisManager redisManager) {
        this.redisManager = redisManager;
    }

    public void start() {
        executorService.execute(() -> {
            try(Jedis jedis = redisManager.getConnection()) {
                jedisPubSub = new JedisPubSub() {
                    @Override
                    public  void onMessage(String channel, String message) {
                        AnticheatFlagMessage flagMessage = gson.fromJson(message, AnticheatFlagMessage.class);

                        // TODO ALERT + 1x  /sus entry
                        sendAlert(flagMessage);
                    }
                };

                jedis.subscribe(jedisPubSub, CHANNEL);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private void sendAlert(AnticheatFlagMessage flagMessage) {
        Goliath.getInstance().getAnticheatAlert().sendAlert(flagMessage);
    }

    public void shutdown() {
        if (jedisPubSub != null && jedisPubSub.isSubscribed()) {
            jedisPubSub.unsubscribe();
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

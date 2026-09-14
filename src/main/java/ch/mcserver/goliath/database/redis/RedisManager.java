package ch.mcserver.goliath.database.redis;

import ch.mcserver.goliath.Goliath;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RedisManager {

    private JedisPool pool;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public void connect() {
        String ipAddress = Goliath.config.node("redis", "ipaddress").getString();
        int port = Goliath.config.node("redis", "port").getInt();
        String password =  Goliath.config.node("redis", "password").getString();
        try {
            if (password == null || password.isBlank()) {
                pool = new JedisPool(ipAddress, port);
                Goliath.LOGGER.info("Connected to Redis.");
            } else {
                pool = new JedisPool(ipAddress, port, null, password);

            }
        } catch (Exception e) {
            Goliath.LOGGER.warn(e.getMessage());
        }
    }

    public Jedis getConnection() {
        return pool.getResource();
    }

    public void close() {
        try {
            executorService.shutdown();
            boolean finished = executorService.awaitTermination(5, TimeUnit.SECONDS);
            if (!finished) {
                executorService.shutdownNow();
            }
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
        }finally {
            if (pool != null) {
                pool.close();
                Goliath.LOGGER.info("Closed Redis.");
            }
        }


    }

    public void publish(String channel, String message) {
        executorService.execute(() -> {
            try (Jedis jedis = getConnection()) {
                jedis.publish(channel, message);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }
}


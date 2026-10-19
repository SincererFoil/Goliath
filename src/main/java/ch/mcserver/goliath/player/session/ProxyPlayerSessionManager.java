package ch.mcserver.goliath.player.session;

import ch.mcserver.goliath.Goliath;
import ch.mcserver.goliath.database.redis.RedisManager;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

public class ProxyPlayerSessionManager {

    private final RedisManager redisManager;

    private static final String SESSIONID_PROXY_CHANNEL = "goliath:session:id:";

    private static final String PLAYER_SESSIONID_CHANNEL = "goliath:session:player:";

    private final String proxyId;


    public ProxyPlayerSessionManager(RedisManager redisManager, String proxyId) {
        this.redisManager = redisManager;
        this.proxyId = proxyId;
    }

    public String getProxyId(String sessionId) {
        try (Jedis jedis = redisManager.getConnection()) {
            return jedis.get(SESSIONID_PROXY_CHANNEL + sessionId);
        }
    }



    public boolean isCurrentSession(UUID playerUuid, String sessionId) {
        try (Jedis jedis = redisManager.getConnection()) {
            return sessionId != null && sessionId.equals(jedis.get(PLAYER_SESSIONID_CHANNEL + playerUuid));
        }
    }

    public void removeSession(UUID playerUuid, String sessionId) {

        if (sessionId == null) {
            Goliath.LOGGER.warn("Couldn't remove player session because sessionId is null");
            return;
        }

        try (Jedis jedis = redisManager.getConnection()) {
            if (proxyId.equals(jedis.get(SESSIONID_PROXY_CHANNEL + sessionId)) && sessionId.equals(jedis.get(PLAYER_SESSIONID_CHANNEL + playerUuid))) {
                jedis.del(PLAYER_SESSIONID_CHANNEL + playerUuid);
                jedis.del(SESSIONID_PROXY_CHANNEL + sessionId);
            }
        }
    }

    public boolean hasSession(UUID playerUuid) {
        try (Jedis jedis = redisManager.getConnection()) {
            return jedis.exists(PLAYER_SESSIONID_CHANNEL + playerUuid);
        }
    }

    public String getSession(UUID playerUuid) {
        try (Jedis jedis = redisManager.getConnection()) {
            return jedis.get(PLAYER_SESSIONID_CHANNEL + playerUuid);
        }
    }

    public boolean createSession(UUID playerUuid) {
        String sessionId = buildSessionId(playerUuid);

        if (sessionId == null) {
            Goliath.LOGGER.info("SessionManager wasn't able to create session id ");
            return false;
        }

        try (Jedis jedis = redisManager.getConnection()) {
            String response = jedis.set(PLAYER_SESSIONID_CHANNEL + playerUuid, sessionId, SetParams.setParams().nx());

            if (!"OK".equals(response)) {
                return false;
            }

            String responseProxy = jedis.set(SESSIONID_PROXY_CHANNEL + sessionId, proxyId, SetParams.setParams().nx());

            if (!"OK".equals(responseProxy)) {
                if (sessionId.equals(jedis.get(PLAYER_SESSIONID_CHANNEL + playerUuid))) {
                    jedis.del(PLAYER_SESSIONID_CHANNEL + playerUuid);
                }
                return false;
            }

            return true;
        }
    }

    private String buildSessionId(UUID playerUuid) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] sessionByteKey = (playerUuid.toString() + proxyId + UUID.randomUUID().toString()).getBytes(StandardCharsets.UTF_8);

            return HexFormat.of().formatHex(digest.digest(sessionByteKey));
        } catch (NoSuchAlgorithmException e) {
            Goliath.LOGGER.info("Couldn't load the algorithm " + e.toString());
            return null;
        }
    }


}

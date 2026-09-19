package ch.mcserver.goliath.player.session;

import ch.mcserver.goliath.Goliath;
import ch.mcserver.goliath.database.redis.RedisManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

public class ProxyPlayerSessionManager {

    private final RedisManager redisManager;

    private final String SESSIONID_PROXY_CHANNEL = "goliath:session:id:";

    private final String PLAYER_SESSIONID_CHANNEL = "goliath:session:player:";

    private final String proxyId;

    public ProxyPlayerSessionManager(RedisManager redisManager, String proxyId) {
        this.redisManager = redisManager;
        this.proxyId = proxyId;
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

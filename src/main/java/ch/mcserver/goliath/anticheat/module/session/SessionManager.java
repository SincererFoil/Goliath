package ch.mcserver.goliath.anticheat.module.session;

import ch.mcserver.goliath.Goliath;
import ch.mcserver.goliath.database.redis.RedisManager;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;


public class SessionManager {

    private final RedisManager redisManager;

    private final static String PLAYER_SESSION_CHANNEL = "goliath:session:proxy:";

    public final String proxyId;


    public SessionManager(RedisManager redisManager, String proxyId) {
        this.redisManager = redisManager;
        this.proxyId = proxyId;
    }

    public void disconnectPlayer(DisconnectType type, Player player) {

        Component proxyRequestJoinCacheLoginMessage = (Component.text("You are already online", NamedTextColor.RED)
                .appendNewline()
                .append(Component.text("You are connected to proxy: ", NamedTextColor.GRAY))
                .append(Component.text(getPlayersProxyId(player.getUniqueId()), NamedTextColor.WHITE))
                .appendNewline()
                .append(Component.text("Connect reason: ", NamedTextColor.GRAY))
                .append(Component.text("proxy request-join-cache login", NamedTextColor.WHITE)));

        if (type == DisconnectType.PROXY_REQUEST_JOIN_CACHE_LOGIN) {
            player.disconnect(proxyRequestJoinCacheLoginMessage);
        }
    }

    public void onConnect(Player player) {

        if (!registerPlayer(player.getUniqueId())) {
            disconnectPlayer(DisconnectType.PROXY_REQUEST_JOIN_CACHE_LOGIN, player);
        }

    }

    public void onDisconnect(UUID playerUuid) {
        unRegisterPlayer(playerUuid);
    }

    public String getPlayersProxyId(UUID playerUuid) {

        try (Jedis jedis = redisManager.getConnection()) {
            return jedis.get(PLAYER_SESSION_CHANNEL + playerUuid.toString());
        }

    }

    public boolean  registerPlayer(UUID playerUuid) {
        try (Jedis jedis = redisManager.getConnection()) {
            String result = jedis.set(PLAYER_SESSION_CHANNEL + playerUuid.toString(), proxyId, SetParams.setParams().nx());

            return "OK".equals(result);
        }
    }

    public void unRegisterPlayer(UUID playerUuid) {
        if (proxyId.equals(getPlayersProxyId(playerUuid))) {
            try (Jedis jedis = redisManager.getConnection()) {
                jedis.del(PLAYER_SESSION_CHANNEL + playerUuid.toString());
            }
        }

    }

    public boolean isOnline(UUID playerUuid) {
        try (Jedis jedis = redisManager.getConnection()) {
            return jedis.exists(PLAYER_SESSION_CHANNEL + playerUuid.toString());
        }
    }





}

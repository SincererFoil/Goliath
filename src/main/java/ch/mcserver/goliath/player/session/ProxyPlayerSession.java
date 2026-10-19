package ch.mcserver.goliath.player.session;

import java.util.UUID;

public class ProxyPlayerSession {

    private final UUID playerUuid;

    private final String sessionId;

    private final String proxyId;

    private final long connectedAt;


    public ProxyPlayerSession(UUID playerUuid, String sessionId, String proxyId, long connectedAt) {
        this.playerUuid = playerUuid;
        this.sessionId = sessionId;
        this.proxyId = proxyId;
        this.connectedAt = connectedAt;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getProxyId() {
        return proxyId;
    }

    public Long getConnectedAt() {
        return connectedAt;
    }
}

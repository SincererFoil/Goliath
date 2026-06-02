package ch.mcserver.goliath.player;

import ch.mcserver.goliath.player.punishments.PlayerPunishment;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProxyPlayerObject {

    private UUID uuid;

    private String name;
    private String prefix;

    private String currentServer;

    private boolean sfmode;
    private boolean debugMode;
    private boolean gmsp;
    private boolean vanished;

    private float flySpeed;

    private long firstJoin;
    private long lastJoin;

    private List<PlayerPunishment> punishments = new ArrayList<>();

    public ProxyPlayerObject(
            UUID uuid,
            String name,
            String prefix,
            String currentServer,
            boolean sfmode,
            boolean debugMode,
            boolean gmsp,
            boolean vanished,
            float flySpeed,
            long firstJoin,
            long lastJoin,
            List<PlayerPunishment> punishments
    ) {

        this.uuid = uuid;
        this.name = name;
        this.prefix = prefix;

        this.currentServer = currentServer;

        this.sfmode = sfmode;
        this.debugMode = debugMode;
        this.gmsp = gmsp;
        this.vanished = vanished;

        this.flySpeed = flySpeed;

        this.firstJoin = firstJoin;
        this.lastJoin = lastJoin;

        if (punishments != null) {
            this.punishments = punishments;
        }
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getCurrentServer() {
        return currentServer;
    }

    public boolean isSfmode() {
        return sfmode;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public boolean isGmsp() {
        return gmsp;
    }

    public boolean isVanished() {
        return vanished;
    }

    public float getFlySpeed() {
        return flySpeed;
    }

    public long getFirstJoin() {
        return firstJoin;
    }

    public long getLastJoin() {
        return lastJoin;
    }

    public List<PlayerPunishment> getPunishments() {
        return punishments;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setCurrentServer(String currentServer) {
        this.currentServer = currentServer;
    }

    public void setSfmode(boolean sfmode) {
        this.sfmode = sfmode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public void setGmsp(boolean gmsp) {
        this.gmsp = gmsp;
    }

    public void setVanished(boolean vanished) {
        this.vanished = vanished;
    }

    public void setFlySpeed(float flySpeed) {
        this.flySpeed = flySpeed;
    }

    public void setFirstJoin(long firstJoin) {
        this.firstJoin = firstJoin;
    }

    public void setLastJoin(long lastJoin) {
        this.lastJoin = lastJoin;
    }

    public void setPunishments(List<PlayerPunishment> punishments) {
        this.punishments = punishments;
    }

    public void addPunishment(PlayerPunishment punishment) {
        this.punishments.add(punishment);
    }

    public void removePunishment(PlayerPunishment punishment) {
        this.punishments.remove(punishment);
    }
}
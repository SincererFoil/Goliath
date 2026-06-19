package ch.mcserver.goliath.database.mysql.repository;

import java.sql.Timestamp;
import java.util.UUID;

public class PlayerLocationObject {

    private final UUID uuid;
    private final Timestamp updatedAt;
    private final float yaw;
    private final float pitch;
    private final int x;
    private final int y;
    private final int z;
    private final String serverName;

    public PlayerLocationObject(
            UUID uuid,
            Timestamp updatedAt,
            float yaw,
            float pitch,
            int x,
            int y,
            int z,
            String serverName
    ) {
        this.uuid = uuid;
        this.updatedAt = updatedAt;
        this.yaw = yaw;
        this.pitch = pitch;
        this.x = x;
        this.y = y;
        this.z = z;
        this.serverName = serverName;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public String getServerName() {
        return serverName;
    }
}
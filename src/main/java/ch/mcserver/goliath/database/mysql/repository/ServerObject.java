package ch.mcserver.goliath.database.mysql.repository;

import ch.mcserver.goliath.servermanager.Region;
import ch.mcserver.goliath.servermanager.ServerStatus;
import ch.mcserver.goliath.servermanager.ServerType;

import java.sql.Timestamp;

public class ServerObject {

    private int serverId;
    private String serverName;
    private ServerStatus serverStatus;
    private Region serverRegion;
    private ServerType serverType;
    private int playerCount;
    private Timestamp updatedAt;

    public ServerObject(int serverId, String serverName, ServerStatus serverStatus, Region serverRegion, ServerType serverType, int playerCount, Timestamp updatedAt) {
        this.serverId = serverId;
        this.serverName = serverName;
        this.serverStatus = serverStatus;
        this.serverRegion = serverRegion;
        this.serverType = serverType;
        this.playerCount = playerCount;
        this.updatedAt = updatedAt;
    }

    public  int getServerId() {
        return  serverId;
    }

    public void  setServerId(int serverId) {
        this.serverId = serverId;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public ServerStatus getServerStatus() {
        return serverStatus;
    }

    public void setServerStatus(ServerStatus serverStatus) {
        this.serverStatus = serverStatus;
    }

    public Region getServerRegion() {
        return serverRegion;
    }

    public void setServerRegion(Region serverRegion) {
        this.serverRegion = serverRegion;
    }

    public ServerType getServerType() {
        return serverType;
    }

    public void setServerType(ServerType serverType) {
        this.serverType = serverType;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
    }

    public Timestamp getUpdatedAt() {
        return  updatedAt;
    }

    public void  setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}

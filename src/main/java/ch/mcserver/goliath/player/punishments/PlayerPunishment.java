package ch.mcserver.goliath.player.punishments;


import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.UUID;

public class PlayerPunishment {
    private int offenseLevel;
    private String reason;
    private String ipAddress;
    private String punishedBy;
    private ZonedDateTime createdAt;
    private ZonedDateTime expiresAt;
    private boolean wiped;
    private String staffNote;
    private boolean permanent;

    public PlayerPunishment(int offenseLevel, String reason, String ipAddress, String punishedBy, ZonedDateTime createdAt, ZonedDateTime expiresAt, boolean wiped, String staffNote,  boolean permanent) {
        this.offenseLevel = offenseLevel;
        this.reason = reason;
        this.ipAddress = ipAddress;
        this.punishedBy = punishedBy;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.wiped = wiped;
        this.staffNote = staffNote;
        this.permanent = permanent;
    }

    public int getOffenseLevel() {
        return offenseLevel;
    }

    public String getReason() {
        return reason;
    }

    public String getStaffNote() {
        return staffNote;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getPunishedBy() {
        return punishedBy;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public ZonedDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isWiped() {
        return wiped;
    }

    public void  setStaffNote(String staffNote) {
        this.staffNote = staffNote;
    }

    public boolean isPermanent() {
        return permanent;
    }
    public void setPermanent(boolean permanent) {
        this.permanent = permanent;
    }

    public void setOffenseLevel(int offenseLevel) {
        this.offenseLevel = offenseLevel;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setPunishedBy(String punishedBy) {
        this.punishedBy = punishedBy;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setExpiresAt(ZonedDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public void setWiped(boolean wiped) {
        this.wiped = wiped;
    }
}


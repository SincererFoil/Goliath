package ch.mcserver.goliath.command.moderation.punish;

import java.time.ZonedDateTime;
import java.util.UUID;

public record PunishEvent(UUID targetUuid, String reason, String bannedBy, String banId, Long expiresAt, Long  issuedAt) {
    public boolean isPermanent() {
        return expiresAt == null;
    }
}

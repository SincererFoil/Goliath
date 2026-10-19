package ch.mcserver.goliath.anticheat;

import java.util.UUID;

public record AnticheatFlagMessage(
        UUID playerUuid,
        String playerName,
        String checkName,
        int violations,
        String details,
        String serverName,
        long timestamp
) {
}

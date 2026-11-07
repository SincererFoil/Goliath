package ch.mcserver.goliath.rtp;

import java.util.UUID;

public record RandomTeleportRequestData(UUID playerUuid, Region region, String sourceServer, String dimension, UUID requestUuid) {
}

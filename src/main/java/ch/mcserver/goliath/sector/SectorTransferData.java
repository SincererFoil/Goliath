package ch.mcserver.goliath.sector;

import java.util.UUID;

public record SectorTransferData(UUID transferUuid, UUID playerUuid, String sourceServer, String targetServer, String dimension, double x, double y, double z, float yaw, float pitch) {
}
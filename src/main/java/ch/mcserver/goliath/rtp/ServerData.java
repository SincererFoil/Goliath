package ch.mcserver.goliath.rtp;

public record ServerData(
        String serverName,
        String instanceId,
        String region,
        String dimension,
        int players,
        String status,
        double tps,
        double mspt,
        long timestamp
) {
}
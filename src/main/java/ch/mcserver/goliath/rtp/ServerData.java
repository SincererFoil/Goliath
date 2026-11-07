package ch.mcserver.goliath.rtp;

public record ServerData(
        String serverName,
        String instanceId,
        int players,
        String status,
        double tps,
        double mspt,
        long timestamp
) {
}
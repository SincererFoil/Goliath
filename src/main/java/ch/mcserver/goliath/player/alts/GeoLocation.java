package ch.mcserver.goliath.player.alts;

public record GeoLocation(
        String city,
        String region,
        String country,
        int accuracyRadius
) {
}
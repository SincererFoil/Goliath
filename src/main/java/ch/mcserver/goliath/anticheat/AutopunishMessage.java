package ch.mcserver.goliath.anticheat;

public record AutopunishMessage(
        AnticheatFlagMessage flagData,
        String punishReason
) {
}

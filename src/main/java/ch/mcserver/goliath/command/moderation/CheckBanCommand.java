package ch.mcserver.goliath.command.moderation;

import ch.mcserver.goliath.Goliath;
import ch.mcserver.goliath.database.mysql.repository.PlayerRepository;
import ch.mcserver.goliath.player.ProxyPlayerObject;
import ch.mcserver.goliath.player.punishments.PlayerPunishment;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class CheckBanCommand implements SimpleCommand {

    private static final ZoneId ZONE = ZoneId.of("Europe/Zurich");

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length != 1) {
            invocation.source().sendMessage(Component.text(
                    "Wrong Usage: /checkban <player>",
                    NamedTextColor.RED
            ));
            return;
        }

        String targetName = args[0];
        PlayerRepository playerRepository = Goliath.playerRepository;

        if (!playerRepository.existsByUsername(targetName)) {
            invocation.source().sendMessage(Component.text(
                    "Player not found!",
                    NamedTextColor.RED
            ));
            return;
        }

        ProxyPlayerObject playerObject = playerRepository.loadPlayerByUsername(targetName);

        if (playerObject == null || playerObject.getPunishments() == null) {
            invocation.source().sendMessage(Component.text(
                    "Player is not banned!",
                    NamedTextColor.RED
            ));
            return;
        }

        ZonedDateTime now = ZonedDateTime.now(ZONE);
        PlayerPunishment activePunishment = null;

        for (PlayerPunishment punishment : playerObject.getPunishments()) {
            if (!punishment.isActive()) {
                continue;
            }

            boolean permanent = punishment.isPermanent();
            boolean temporary = punishment.getExpiresAt() != null
                    && punishment.getExpiresAt().isAfter(now);

            if (!permanent && !temporary) {
                continue;
            }

            if (activePunishment == null
                    || punishment.getCreatedAt().isAfter(activePunishment.getCreatedAt())) {
                activePunishment = punishment;
            }
        }

        if (activePunishment == null) {
            invocation.source().sendMessage(Component.text(
                    "Player is not banned!",
                    NamedTextColor.RED
            ));
            return;
        }

        String reason = activePunishment.getReason() == null
                ? "Unknown reason"
                : activePunishment.getReason();

        String staffName = activePunishment.getPunishedBy() == null
                ? "Console"
                : activePunishment.getPunishedBy();

        String staffNote = activePunishment.getStaffNote();

        Component reasonComponent = reason.contains("&")
                ? LegacyComponentSerializer.legacyAmpersand().deserialize(reason)
                : Component.text(reason, NamedTextColor.WHITE);

        if (activePunishment.isPermanent()) {
            Component message = Component.text(playerObject.getName(), NamedTextColor.WHITE)
                    .append(Component.text(" is currently permanently banned. ", NamedTextColor.RED))
                    .append(Component.text("This user was banned by ", NamedTextColor.RED))
                    .append(Component.text(staffName, NamedTextColor.WHITE))
                    .append(Component.text(" for: ", NamedTextColor.RED))
                    .append(reasonComponent);

            if (staffNote != null && !staffNote.isBlank()) {
                message = message
                        .append(Component.text(" Note: ", NamedTextColor.RED))
                        .append(Component.text(staffNote, NamedTextColor.WHITE));
            }

            invocation.source().sendMessage(message);
            return;
        }

        String duration = formatDuration(
                Duration.between(now, activePunishment.getExpiresAt())
        );

        Component message = Component.text(playerObject.getName(), NamedTextColor.WHITE)
                .append(Component.text(" is currently banned for another ", NamedTextColor.RED))
                .append(Component.text(duration, NamedTextColor.WHITE))
                .append(Component.text(". This user was banned by ", NamedTextColor.RED))
                .append(Component.text(staffName, NamedTextColor.WHITE))
                .append(Component.text(" for: ", NamedTextColor.RED))
                .append(reasonComponent);

        if (staffNote != null && !staffNote.isBlank()) {
            message = message
                    .append(Component.text(" Note: ", NamedTextColor.RED))
                    .append(Component.text(staffNote, NamedTextColor.WHITE));
        }

        invocation.source().sendMessage(message);
    }

    private String formatDuration(Duration duration) {
        long totalMinutes = Math.max(0, duration.toMinutes());
        long days = totalMinutes / (24 * 60);
        long hours = (totalMinutes % (24 * 60)) / 60;
        long minutes = totalMinutes % 60;

        StringBuilder builder = new StringBuilder();

        if (days > 0) {
            builder.append(days).append(days == 1 ? " day " : " days ");
        }

        if (hours > 0) {
            builder.append(hours).append(hours == 1 ? " hour " : " hours ");
        }

        if (minutes > 0 || builder.isEmpty()) {
            builder.append(minutes).append(minutes == 1 ? " minute" : " minutes");
        }

        return builder.toString().trim();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("goliath.staff.checkban");
    }
}
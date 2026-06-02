package ch.mcserver.goliath.command.moderation;

import ch.mcserver.goliath.Goliath;
import ch.mcserver.goliath.database.mysql.repository.PlayerRepository;
import ch.mcserver.goliath.player.ProxyPlayerObject;
import ch.mcserver.goliath.player.punishments.PlayerPunishment;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

public class CheckBanCommand implements SimpleCommand {

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();

        if (!(invocation.source() instanceof Player player)) {
            return;
        }

        if (args.length != 1) {
            player.sendMessage(Component.text("No player found!", NamedTextColor.RED));
            return;
        }

        String targetRawName = args[0];
        PlayerRepository playerRepository = Goliath.playerRepository;

        if (!playerRepository.existsByUsername(targetRawName)) {
            player.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
            return;
        }

        ProxyPlayerObject playerObject = playerRepository.loadPlayerByUsername(targetRawName);
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());

        for (PlayerPunishment punishment : playerObject.getPunishments()) {

            String reason = punishment.getReason();
            String staffName = punishment.getPunishedBy();

            if (reason == null) {
                reason = "Unknown reason";
            }

            if (staffName == null) {
                staffName = "Console";
            }

            if (punishment.isPermanent()) {
                player.sendMessage(
                        Component.text(targetRawName, NamedTextColor.WHITE)
                                .append(Component.text(" is currently permanently banned", NamedTextColor.RED))
                                .append(Component.text(". This user was banned by ", NamedTextColor.RED))
                                .append(Component.text(staffName, NamedTextColor.WHITE))
                                .append(Component.text(" for: ", NamedTextColor.RED))
                                .append(Component.text(reason, NamedTextColor.RED))
                );
                return;
            }

            if (punishment.getExpiresAt() == null) {
                continue;
            }

            if (!punishment.getExpiresAt().isAfter(now)) {
                continue;
            }

            String durationText = formatDuration(Duration.between(now, punishment.getExpiresAt()));

            player.sendMessage(
                    Component.text(targetRawName, NamedTextColor.WHITE)
                            .append(Component.text(" is currently banned for another ", NamedTextColor.RED))
                            .append(Component.text(durationText, NamedTextColor.WHITE))
                            .append(Component.text(". This user was banned by ", NamedTextColor.RED))
                            .append(Component.text(staffName, NamedTextColor.WHITE))
                            .append(Component.text(" for: ", NamedTextColor.RED))
                            .append(Component.text(reason, NamedTextColor.RED))
            );

            return;
        }

        player.sendMessage(Component.text("Player is not banned", NamedTextColor.RED));
    }

    private String formatDuration(Duration duration) {
        long totalMinutes = duration.toMinutes();

        long days = totalMinutes / (24 * 60);
        long hours = (totalMinutes % (24 * 60)) / 60;
        long minutes = totalMinutes % 60;

        StringBuilder builder = new StringBuilder();

        if (days > 0) {
            builder.append(days).append(" day ");
        }

        if (hours > 0) {
            builder.append(hours).append(" hours ");
        }

        if (minutes > 0 || builder.isEmpty()) {
            builder.append(minutes).append(" minutes");
        }

        return builder.toString().trim();
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("GoliathCommand.checkban");
    }
}
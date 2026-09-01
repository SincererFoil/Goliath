package ch.mcserver.goliath.command.moderation;

import ch.mcserver.goliath.database.mysql.repository.PlayerRepository;
import ch.mcserver.goliath.player.ProxyPlayerObject;
import ch.mcserver.goliath.player.punishments.PlayerPunishment;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BanHistoryCommand implements SimpleCommand {

    private static final ZoneId ZONE = ZoneId.of("Europe/Zurich");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final PlayerRepository playerRepository;
    private final ProxyServer proxy;

    public BanHistoryCommand(PlayerRepository playerRepository, ProxyServer proxy) {
        this.playerRepository = playerRepository;
        this.proxy = proxy;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length != 1) {
            invocation.source().sendMessage(Component.text("Wrong Usage: /banhistory <player>", NamedTextColor.RED));
            return;
        }

        ProxyPlayerObject player = playerRepository.loadPlayerByUsername(args[0]);

        if (player == null) {
            invocation.source().sendMessage(Component.text("Player has never logged in.", NamedTextColor.RED));
            return;
        }

        List<PlayerPunishment> punishments = player.getPunishments();

        if (punishments == null || punishments.isEmpty()) {
            invocation.source().sendMessage(Component.text("This player has no ban history.", NamedTextColor.RED));
            return;
        }

        ZonedDateTime now = ZonedDateTime.now(ZONE);
        Component message = Component.empty();

        for (int i = 0; i < punishments.size(); i++) {
            PlayerPunishment punishment = punishments.get(punishments.size() - 1 - i);

            String reason = punishment.getReason() == null ? "Unknown reason" : punishment.getReason();
            String staff = punishment.getPunishedBy() == null ? "Console" : punishment.getPunishedBy();
            String status = getStatus(punishment, now);

            Component reasonComponent = reason.contains("&")
                    ? LegacyComponentSerializer.legacyAmpersand().deserialize(reason)
                    : Component.text(reason, NamedTextColor.RED);

            Component entry = Component.text((i + 1) + ". ", NamedTextColor.RED)
                    .append(Component.text(player.getName(), NamedTextColor.WHITE));

            if (punishment.isPermanent()) {
                if (punishment.isActive()) {
                    entry = entry.append(Component.text(" is currently permanently banned. ", NamedTextColor.RED));
                } else {
                    entry = entry.append(Component.text(" was permanently banned. ", NamedTextColor.RED));
                }
            } else {
                String length = formatBanLength(punishment);

                entry = entry.append(Component.text(" was banned for ", NamedTextColor.RED))
                        .append(Component.text(length, NamedTextColor.WHITE))
                        .append(Component.text(". ", NamedTextColor.RED));
            }

            entry = entry.append(Component.text("This user was banned by ", NamedTextColor.RED))
                    .append(Component.text(staff, NamedTextColor.WHITE))
                    .append(Component.text(" for: ", NamedTextColor.RED))
                    .append(reasonComponent)
                    .hoverEvent(Component.text(
                            "Ban ID: " + punishment.getBanId()
                                    + "\nStatus: " + status
                                    + "\nDate: " + punishment.getCreatedAt().format(FORMATTER)
                                    + "\nStaff note: " + getStaffNote(punishment),
                            NamedTextColor.GRAY
                    ));

            message = message.append(entry);

            if (i < punishments.size() - 1) {
                message = message.appendNewline();
            }
        }

        invocation.source().sendMessage(message);
    }

    private String formatBanLength(PlayerPunishment punishment) {
        if (punishment.getExpiresAt() == null) {
            return "an unknown duration";
        }

        Duration duration = Duration.between(punishment.getCreatedAt(), punishment.getExpiresAt());
        long totalMinutes = Math.max(0, duration.toMinutes());
        long days = totalMinutes / (24 * 60);
        long hours = (totalMinutes % (24 * 60)) / 60;
        long minutes = totalMinutes % 60;

        if (days > 0) {
            return days + (days == 1 ? " day" : " days");
        }

        if (hours > 0) {
            return hours + (hours == 1 ? " hour" : " hours");
        }

        return minutes + (minutes == 1 ? " minute" : " minutes");
    }

    private String getStatus(PlayerPunishment punishment, ZonedDateTime now) {
        if (!punishment.isActive()) {
            return "Unbanned";
        }

        if (punishment.isPermanent()) {
            return "Active";
        }

        if (punishment.getExpiresAt() != null && punishment.getExpiresAt().isAfter(now)) {
            return "Active";
        }

        return "Expired";
    }

    private String getStaffNote(PlayerPunishment punishment) {
        if (punishment.getStaffNote() == null || punishment.getStaffNote().isBlank()) {
            return "None";
        }

        return punishment.getStaffNote();
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length <= 1) {
            String input = args.length == 0 ? "" : args[0].toLowerCase();

            return proxy.getAllPlayers().stream().map(Player::getUsername).filter(name -> name.toLowerCase().startsWith(input)).toList();

        }

        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("goliath.staff.banhistory");
    }
}
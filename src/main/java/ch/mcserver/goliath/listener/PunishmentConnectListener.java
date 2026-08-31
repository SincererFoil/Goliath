package ch.mcserver.goliath.listener;

import ch.mcserver.goliath.Goliath;
import ch.mcserver.goliath.database.mysql.repository.PlayerRepository;
import ch.mcserver.goliath.player.ProxyPlayerObject;
import ch.mcserver.goliath.player.punishments.PlayerPunishment;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static ch.mcserver.goliath.command.admin.GoliathCommand.maintenance;

public class PunishmentConnectListener {

    private static final ZoneId ZONE = ZoneId.of("Europe/Zurich");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @Subscribe
    public void onPreLogin(PreLoginEvent event) {
        PlayerRepository playerRepository = Goliath.playerRepository;
        String username = event.getUsername();

        if (!playerRepository.existsByUsername(username)) {
            return;
        }

        ProxyPlayerObject targetObject = playerRepository.loadPlayerByUsername(username);

        if (targetObject == null || targetObject.getPunishments() == null) {
            return;
        }

        ZonedDateTime now = ZonedDateTime.now(ZONE);

        for (PlayerPunishment punishment : targetObject.getPunishments()) {
            if (!punishment.isActive()) {
                continue;
            }

            String reason = punishment.getReason() == null
                    ? "Unknown reason"
                    : punishment.getReason();

            if (punishment.isPermanent()) {
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(
                        Component.text(reason, NamedTextColor.RED)
                                .appendNewline()
                                .appendNewline()
                                .append(Component.text("Date: ", NamedTextColor.GRAY))
                                .append(Component.text(punishment.getCreatedAt().format(DATE_FORMATTER), NamedTextColor.WHITE))
                                .appendNewline()
                                .appendNewline()
                                .append(Component.text("Ban ID: ", NamedTextColor.GRAY))
                                .append(Component.text(punishment.getBanId(), NamedTextColor.WHITE))
                                .appendNewline()
                                .appendNewline()
                                .append(Component.text("You may be able to appeal this ban on", NamedTextColor.GRAY))
                                .appendNewline()
                                .append(Component.text("discord.gg/donutsmp", NamedTextColor.WHITE))
                ));
                return;
            }

            if (punishment.getExpiresAt() == null || !punishment.getExpiresAt().isAfter(now)) {
                continue;
            }

            Duration remaining = Duration.between(now, punishment.getExpiresAt());

            long totalMinutes = Math.max(0, remaining.toMinutes());
            long days = totalMinutes / (24 * 60);
            long hours = (totalMinutes % (24 * 60)) / 60;
            long minutes = totalMinutes % 60;

            String formatted = days + " Days " + hours + " Hours " + minutes + " Minutes";

            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(
                    Component.text(reason, NamedTextColor.RED)
                            .appendNewline()
                            .appendNewline()
                            .append(Component.text("Time Left: ", NamedTextColor.GRAY))
                            .append(Component.text(formatted, NamedTextColor.WHITE))
                            .appendNewline()
                            .appendNewline()
                            .append(Component.text("Ban ID: ", NamedTextColor.GRAY))
                            .append(Component.text(punishment.getBanId(), NamedTextColor.WHITE))
                            .appendNewline()
                            .appendNewline()
                            .append(Component.text("You may be able to appeal this ban on", NamedTextColor.GRAY))
                            .appendNewline()
                            .append(Component.text("discord.gg/donutsmp", NamedTextColor.WHITE))
            ));
            return;
        }
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();

        if (maintenance && !player.hasPermission("goliath.maintenance.bypass")) {
            Component message = Component.text("We are under maintenance.", NamedTextColor.RED)
                    .appendNewline()
                    .append(Component.text("For more information check the updates channel.", NamedTextColor.WHITE))
                    .appendNewline()
                    .append(Component.text("Join our Discord: ", NamedTextColor.GRAY))
                    .append(Component.text("discord.gg/donutsmp", NamedTextColor.YELLOW));

            event.setResult(ResultedEvent.ComponentResult.denied(message));
        }
    }
}
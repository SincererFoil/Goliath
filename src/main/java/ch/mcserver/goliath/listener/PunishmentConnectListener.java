package ch.mcserver.goliath.listener;

import ch.mcserver.goliath.Goliath;
import ch.mcserver.goliath.database.mysql.repository.PlayerRepository;
import ch.mcserver.goliath.player.ProxyPlayerObject;
import ch.mcserver.goliath.player.punishments.PlayerPunishment;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class PunishmentConnectListener {

    private static final ZoneId ZONE = ZoneId.of("Europe/Zurich");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd-yyyy");

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
            if (punishment.isPermanent()) {
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(
                        Component.text(punishment.getReason(), NamedTextColor.RED)
                                .appendNewline()
                                .append(Component.text("Date: ", NamedTextColor.GRAY))
                                .append(Component.text(punishment.getCreatedAt().format(DATE_FORMATTER), NamedTextColor.WHITE))
                                .appendNewline()
                                .appendNewline()
                                .append(Component.text("Ban ID: ", NamedTextColor.GRAY))
                                .append(Component.text(punishment.getBanId(), NamedTextColor.WHITE))
                                .appendNewline()
                                .appendNewline()
                                .append(Component.text("You may be able to appeal to this ban on", NamedTextColor.GRAY))
                                .appendNewline()
                                .append(Component.text("discord.gg/donutsmp", NamedTextColor.WHITE))
                ));
                return;
            }

            if (punishment.getExpiresAt() == null) {
                continue;
            }

            if (!punishment.getExpiresAt().isAfter(now)) {
                continue;
            }

            Duration remaining = Duration.between(now, punishment.getExpiresAt());

            long totalMinutes = Math.max(0, remaining.toMinutes());
            long days = totalMinutes / (24 * 60);
            long hours = (totalMinutes % (24 * 60)) / 60;
            long minutes = totalMinutes % 60;

            String formatted = days + " Days " + hours + " Hours " + minutes + " Minutes";

            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(
                    Component.text(punishment.getReason(), NamedTextColor.RED)
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
}
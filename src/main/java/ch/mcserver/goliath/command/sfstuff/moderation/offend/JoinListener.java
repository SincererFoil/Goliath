package ch.mcserver.goliath.command.sfstuff.moderation.offend;

import ch.mcserver.goliath.Goliath;
import ch.mcserver.goliath.database.repository.mysql.PlayerRepository;
import ch.mcserver.goliath.player.ProxyPlayerObject;
import ch.mcserver.goliath.player.punishments.PlayerPunishment;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class JoinListener {

    private static final ZoneId ZONE = ZoneId.of("Europe/Zurich");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

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
                        Component.text("You are permanently banned from this server!", NamedTextColor.RED)
                                .appendNewline()
                                .append(Component.text("Reason: " + punishment.getReason(), NamedTextColor.WHITE))
                ));
                return;
            }

            if (punishment.getExpiresAt() == null) {
                continue;
            }

            if (!punishment.getExpiresAt().isAfter(now)) {
                continue;
            }

            String dateText = punishment.getCreatedAt().format(FORMATTER);

            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(
                    Component.text("You are banned from this server!", NamedTextColor.RED)
                            .appendNewline()
                            .append(Component.text("Expires in: " + punishment.getExpiresAt().format(FORMATTER), NamedTextColor.WHITE))
                            .appendNewline()
                            .append(Component.text("Date: " + dateText, NamedTextColor.WHITE))
                            .appendNewline()
                            .append(Component.text("Reason: " + punishment.getReason(), NamedTextColor.WHITE))
            ));
            return;
        }
    }
}
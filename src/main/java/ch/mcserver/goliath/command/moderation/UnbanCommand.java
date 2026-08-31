package ch.mcserver.goliath.command.moderation;

import ch.mcserver.goliath.Goliath;
import ch.mcserver.goliath.database.mysql.repository.PlayerRepository;
import ch.mcserver.goliath.player.ProxyPlayerObject;
import ch.mcserver.goliath.player.punishments.PlayerPunishment;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class UnbanCommand implements SimpleCommand {

    private static final ZoneId ZONE = ZoneId.of("Europe/Zurich");

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length != 1) {
            invocation.source().sendMessage(Component.text(
                    "Wrong Usage: /unban <player>",
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
        boolean foundActiveBan = false;

        for (PlayerPunishment punishment : playerObject.getPunishments()) {
            if (!punishment.isActive()) {
                continue;
            }

            boolean permanentBan = punishment.isPermanent();
            boolean temporaryBan = punishment.getExpiresAt() != null
                    && punishment.getExpiresAt().isAfter(now);

            if (permanentBan || temporaryBan) {
                punishment.setActive(false);
                foundActiveBan = true;
            }
        }

        if (!foundActiveBan) {
            invocation.source().sendMessage(Component.text(
                    "Player is not banned!",
                    NamedTextColor.RED
            ));
            return;
        }

        playerRepository.save(playerObject);

        invocation.source().sendMessage(
                Component.text("Unbanned player ", NamedTextColor.GREEN)
                        .append(Component.text(playerObject.getName(), NamedTextColor.WHITE))
        );
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("goliath.staff.unban");
    }
}
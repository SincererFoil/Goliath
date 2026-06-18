package ch.mcserver.goliath.command.moderation;

import ch.mcserver.goliath.Goliath;
import ch.mcserver.goliath.database.mysql.repository.PlayerRepository;
import ch.mcserver.goliath.player.ProxyPlayerObject;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class UnbanCommand implements SimpleCommand {

    @Override
    public void execute(Invocation invocation) {

        String[] args = invocation.arguments();

        if (args.length != 1) {
            invocation.source().sendMessage(
                    Component.text("Player not found!", NamedTextColor.RED)
            );
            return;
        }

        String targetRawName = args[0];

        PlayerRepository playerRepository = Goliath.playerRepository;

        if (!playerRepository.existsByUsername(targetRawName)) {
            invocation.source().sendMessage(
                    Component.text("Player not found!", NamedTextColor.RED)
            );
            return;
        }

        ProxyPlayerObject playerObject =
                playerRepository.loadPlayerByUsername(targetRawName);

        if (playerObject.getPunishments() == null) {
            invocation.source().sendMessage(
                    Component.text("Player is not banned!", NamedTextColor.RED)
            );
            return;
        }

        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());

        boolean removed = playerObject.getPunishments().removeIf(
                punishment ->
                        punishment.isPermanent()
                                || (
                                punishment.getExpiresAt() != null
                                        && punishment.getExpiresAt().isAfter(now)
                        )
        );

        if (!removed) {
            invocation.source().sendMessage(
                    Component.text("Player is not banned!", NamedTextColor.RED)
            );
            return;
        }

        playerRepository.save(playerObject);

        invocation.source().sendMessage(
                Component.text(
                        "Unbanned player ", NamedTextColor.RED
                ).append(Component.text(playerObject.getName(), NamedTextColor.WHITE))
        );
    }



    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("GoliathCommand.unban");
    }
}
package ch.mcserver.goliath.command.sfstuff.moderation.offend;

import ch.mcserver.goliath.Goliath;
import ch.mcserver.goliath.database.repository.mysql.PlayerRepository;
import ch.mcserver.goliath.player.ProxyPlayerObject;
import ch.mcserver.goliath.player.punishments.PlayerPunishment;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Ban implements SimpleCommand {

    private final ProxyServer proxy;

    public Ban(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public void execute(Invocation invocation) {

        String[] args = invocation.arguments();

        if (args.length < 2) {
            invocation.source().sendMessage(
                    Component.text("Usage: /ban <player> <reason>", NamedTextColor.RED)
            );
            return;
        }

        String targetName = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        PlayerRepository playerRepository = Goliath.playerRepository;

        if (!playerRepository.existsByUsername(targetName)) {
            invocation.source().sendMessage(
                    Component.text("Player does not exist", NamedTextColor.RED)
            );
            return;
        }

        ProxyPlayerObject targetObject =
                playerRepository.loadPlayerByUsername(targetName);

        String staffName = "Console";

        if (invocation.source() instanceof Player player) {
            staffName = player.getUsername();
        }

        PlayerPunishment punishment = new PlayerPunishment(
                0,
                "You are permanently banned for " + reason + ".",
                null,
                staffName,
                ZonedDateTime.now(ZoneId.of("Europe/Zurich")),
                null,
                true,
                "null",
                true
        );

        targetObject.getPunishments().add(punishment);

        playerRepository.save(targetObject);

        invocation.source().sendMessage(
                Component.text(
                        "Punishment executed with success.",
                        NamedTextColor.RED
                )
        );

        invocation.source().sendMessage(
                Component.text("Permanently banned player ", NamedTextColor.RED)
                        .append(Component.text(
                                targetObject.getName(),
                                NamedTextColor.WHITE
                        ))
                        .append(Component.text(
                                " with reason:",
                                NamedTextColor.RED
                        ))
        );

        invocation.source().sendMessage(
                Component.text(
                        punishment.getReason(),
                        NamedTextColor.WHITE
                )
        );

        Optional<Player> targetPlayer =
                proxy.getPlayer(targetObject.getUuid());

        if (targetPlayer.isPresent()) {

            Player target = targetPlayer.get();

            target.disconnect(
                    Component.text(
                                    "You are permanently banned from this server!",
                                    NamedTextColor.RED
                            )
                            .appendNewline()
                            .append(Component.text(
                                    "Reason: " + punishment.getReason(),
                                    NamedTextColor.WHITE
                            ))
            );
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {

        String[] args = invocation.arguments();

        if (args.length == 0) {
            return proxy.getAllPlayers()
                    .stream()
                    .map(Player::getUsername)
                    .toList();
        }

        if (args.length == 1) {

            String input = args[0].toLowerCase();

            return proxy.getAllPlayers()
                    .stream()
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .toList();
        }

        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("GoliathCommand.ban");
    }
}

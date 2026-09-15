package ch.mcserver.goliath.command.moderation;

import ch.mcserver.goliath.Goliath;
import ch.mcserver.goliath.database.mysql.repository.PlayerRepository;
import ch.mcserver.goliath.player.ProxyPlayerObject;
import ch.mcserver.goliath.player.punishments.PlayerPunishment;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class BanCommand implements SimpleCommand {

    private final ProxyServer proxy;

    private final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public BanCommand(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public void execute(Invocation invocation) {

        String[] args = invocation.arguments();

        if (args.length < 2) {
            invocation.source().sendMessage(
                    Component.text(
                            "Wrong Usage: /ban <player> <reason> {note: <note>}",
                            NamedTextColor.RED
                    )
            );
            return;
        }

        String targetName = args[0];

        String reasonInput = String.join(
                " ",
                Arrays.copyOfRange(args, 1, args.length)
        );

        int noteIndex = reasonInput.toLowerCase().indexOf("note:");

        String reason = reasonInput.trim();
        String note = null;

        if (noteIndex != -1) {
            reason = reasonInput.substring(0, noteIndex).trim();
            note = reasonInput
                    .substring(noteIndex + "note:".length())
                    .trim();

            if (note.isEmpty()) {
                note = null;
            }
        }

        if (reason.isEmpty()) {
            invocation.source().sendMessage(
                    Component.text(
                            "You need to provide a reason.",
                            NamedTextColor.RED
                    )
            );
            return;
        }

        PlayerRepository playerRepository = Goliath.playerRepository;

        if (!playerRepository.existsByUsername(targetName)) {
            invocation.source().sendMessage(
                    Component.text(
                            "This player has never logged in to the server.",
                            NamedTextColor.RED
                    )
            );
            return;
        }

        ProxyPlayerObject targetObject =
                playerRepository.loadPlayerByUsername(targetName);

        String staffName = "Console";

        if (invocation.source() instanceof Player player) {
            staffName = player.getUsername();
        }

        /*
         * If the target is online, save the current IP address.
         * If the target is offline, no current connection exists,
         * so the punishment is still created without an IP address.
         */
        String ipAddress = proxy.getPlayer(targetObject.getUuid())
                .map(player -> player.getRemoteAddress()
                        .getAddress()
                        .getHostAddress())
                .orElse(null);

        ZonedDateTime date =
                ZonedDateTime.now(ZoneId.of("Europe/Zurich"));

        PlayerPunishment punishment = new PlayerPunishment(
                0,
                reason,
                ipAddress,
                staffName,
                date,
                null,
                true,
                note,
                BanIdGenerator.generateBanId(),
                true,
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
                Component.text(
                                "Permanently banned player ",
                                NamedTextColor.RED
                        )
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
                    Component.text(reason, NamedTextColor.RED)
                            .appendNewline()
                            .appendSpace()
                            .appendNewline()
                            .append(Component.text(
                                    "Date: ",
                                    NamedTextColor.GRAY
                            ))
                            .append(Component.text(
                                    date.format(formatter),
                                    NamedTextColor.WHITE
                            ))
                            .appendNewline()
                            .appendSpace()
                            .appendNewline()
                            .append(Component.text(
                                    "Ban ID: ",
                                    NamedTextColor.GRAY
                            ))
                            .append(Component.text(
                                    punishment.getBanId(),
                                    NamedTextColor.WHITE
                            ))
                            .appendNewline()
                            .append(Component.text(
                                    "You may be able to appeal this ban on",
                                    NamedTextColor.GRAY
                            ))
                            .appendNewline()
                            .append(Component.text(
                                    "discord.gg/mcserver",
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
                    .filter(name ->
                            name.toLowerCase().startsWith(input)
                    )
                    .toList();
        }

        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("goliath.staff.ban");
    }
}
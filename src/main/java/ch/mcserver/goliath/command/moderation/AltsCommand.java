package ch.mcserver.goliath.command.moderation;

import ch.mcserver.goliath.Goliath;
import ch.mcserver.goliath.database.mysql.repository.PlayerIpRepository;
import ch.mcserver.goliath.database.mysql.repository.PlayerRepository;
import ch.mcserver.goliath.player.ProxyPlayerManager;
import ch.mcserver.goliath.player.ProxyPlayerObject;
import ch.mcserver.goliath.player.alts.GeoLocation;
import ch.mcserver.goliath.player.alts.LinkedAccount;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class AltsCommand implements SimpleCommand {

    private final ProxyServer proxy;
    private final PlayerRepository playerRepository;
    private final PlayerIpRepository playerIpRepository;

    public AltsCommand(ProxyServer proxy, PlayerRepository playerRepository, PlayerIpRepository playerIpRepository) {
        this.proxy = proxy;
        this.playerRepository = playerRepository;
        this.playerIpRepository = playerIpRepository;
    }


    @Override
    public void execute(Invocation invocation) {

        String[] args = invocation.arguments();

        if (args.length != 1) {
            invocation.source().sendMessage(Component.text("Wrong Usage: /alts <player>", NamedTextColor.RED));
            return;
        }

        ProxyPlayerObject player = playerRepository.loadPlayerByUsername(args[0]);

        if (player == null) {
            invocation.source().sendMessage(Component.text("Player has never logged in.", NamedTextColor.RED));
            return;
        }

        HashMap<String, NamedTextColor> linkedAccountColorList = new HashMap<>();
        List<LinkedAccount> linkedAccounts = playerIpRepository.getLinkedAccounts(player.getUuid());
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Europe/Zurich"));

        linkedAccounts.forEach(linkedAccount -> {
            ProxyPlayerObject linkedAccountObject = ProxyPlayerManager.getPlayer(linkedAccount.uuid());

            if (linkedAccountObject == null) {
                linkedAccountObject = playerRepository.loadPlayer(linkedAccount.uuid());
            }

            if (linkedAccountObject == null) {
                return;
            }

            boolean banned = linkedAccountObject.getPunishments() != null && linkedAccountObject.getPunishments().stream().anyMatch(punishment -> {
                if (!punishment.isActive()) {
                    return false;
                }

                if (punishment.isPermanent()) {
                    return true;
                }

                return punishment.getExpiresAt() != null && punishment.getExpiresAt().isAfter(now);
            });

            boolean online = proxy.getPlayer(linkedAccount.uuid()).isPresent();

            if (banned) {
                linkedAccountColorList.put(linkedAccountObject.getName(), NamedTextColor.RED);
            } else if (online) {
                linkedAccountColorList.put(linkedAccountObject.getName(), NamedTextColor.GREEN);
            } else {
                linkedAccountColorList.put(linkedAccountObject.getName(), NamedTextColor.GRAY);
            }
        });

        Component message = Component.text("Alts for ", NamedTextColor.WHITE)
                .append(Component.text(player.getName() + ". ", NamedTextColor.GRAY))
                .append(Component.text("[", NamedTextColor.WHITE))
                .append(Component.text("Offline", NamedTextColor.GRAY))
                .append(Component.text("] ", NamedTextColor.WHITE))
                .append(Component.text("[", NamedTextColor.WHITE))
                .append(Component.text("Online", NamedTextColor.GREEN))
                .append(Component.text("] ", NamedTextColor.WHITE))
                .append(Component.text("[", NamedTextColor.WHITE))
                .append(Component.text("Banned", NamedTextColor.RED))
                .append(Component.text("] ", NamedTextColor.WHITE));

        for (int i = 0; i < linkedAccounts.size(); i++) {
            LinkedAccount linkedAccount = linkedAccounts.get(i);
            NamedTextColor color = linkedAccountColorList.getOrDefault(linkedAccount.name(), NamedTextColor.GRAY);

            message = message.append(Component.text(linkedAccount.name(), color));

            if (i < linkedAccounts.size() - 1) {
                message = message.append(Component.text(", ", NamedTextColor.WHITE));
            }
        }

        GeoLocation location = playerIpRepository.getLatestLocation(player.getUuid());

        String locationText = location == null
                ? "Unknown"
                : location.country() + " " + location.city();

        message = message.append(Component.text(" [" + locationText + "] ", NamedTextColor.WHITE))
                .append(Component.text("Linked ", NamedTextColor.WHITE));

        ZonedDateTime lastLinked = linkedAccounts.stream()
                .map(LinkedAccount::lastLinked)
                .max(ZonedDateTime::compareTo)
                .orElse(null);

        if (lastLinked == null) {
            message = message.append(Component.text("[Never]", NamedTextColor.GRAY));
        } else {
            Duration duration = Duration.between(
                    lastLinked,
                    ZonedDateTime.now(ZoneId.of("Europe/Zurich"))
            );
            message = message.append(Component.text("[" + formatLinkedDuration(duration) + " ago ]", NamedTextColor.WHITE))
                    .append(Component.text(" " + lastLinked.toInstant().toEpochMilli() , NamedTextColor.WHITE));
        }

        invocation.source().sendMessage(message);;
    }

    private String formatLinkedDuration(Duration duration) {
        long totalSeconds = Math.max(0, duration.getSeconds());

        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        return days + " d, " + hours + " h, " + minutes + " m, " + seconds + " s";
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
        return invocation.source().hasPermission("goliath.staff.alts");
    }
}

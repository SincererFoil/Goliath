package ch.mcserver.goliath.command.admin;

import ch.mcserver.goliath.Goliath;
import ch.mcserver.goliath.database.mysql.repository.PlayerIpRepository;
import ch.mcserver.goliath.player.ProxyPlayerObject;
import ch.mcserver.goliath.player.alts.GeoLocation;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.Optional;

public class IpCommand implements SimpleCommand {

    private final ProxyServer proxy;

    public IpCommand(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length != 1) {
            invocation.source().sendMessage(
                    Component.text("Wrong usage: /ip <username>", NamedTextColor.RED)
            );
            return;
        }

        ProxyPlayerObject playerObject = Goliath.playerRepository.loadPlayerByUsername(args[0]);

        if (playerObject == null) {
            invocation.source().sendMessage(
                    Component.text("Player has never logged in to the server.", NamedTextColor.RED)
            );
            return;
        }

        PlayerIpRepository ipRepository = Goliath.playerIpRepository;
        GeoLocation location = ipRepository.getLatestLocation(playerObject.getUuid());

        String locationText = location == null
                ? "Unknown location"
                : location.country() + ", " + location.city();

        Optional<Player> onlinePlayer = proxy.getPlayer(playerObject.getUuid());
        Component message = Component.text("IP: ", NamedTextColor.GRAY);

        if (onlinePlayer.isPresent()) {
            String ipAddress = onlinePlayer.get()
                    .getRemoteAddress()
                    .getAddress()
                    .getHostAddress();

            message = message.append(
                    Component.text(ipAddress + " ", NamedTextColor.BLUE)
                            .clickEvent(ClickEvent.copyToClipboard(ipAddress))
            );
        } else {
            message = message.append(
                    Component.text("[OFFLINE] ", NamedTextColor.DARK_GRAY)
            );
        }

        message = message.append(
                Component.text(locationText, NamedTextColor.AQUA)
        );

        invocation.source().sendMessage(message);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length <= 1) {
            String input = args.length == 0 ? "" : args[0].toLowerCase();

            return Goliath.playerRepository.getAllUsernames().stream()
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .sorted()
                    .toList();
        }

        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("goliath.staff.ip");
    }
}
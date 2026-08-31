package ch.mcserver.goliath.command.admin;

import ch.mcserver.goliath.Goliath;
import ch.mcserver.goliath.database.mysql.repository.PlayerIpRepository;
import ch.mcserver.goliath.player.ProxyPlayerManager;
import ch.mcserver.goliath.player.ProxyPlayerObject;
import ch.mcserver.goliath.player.alts.GeoIpService;
import ch.mcserver.goliath.player.alts.GeoLocation;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

public class IpCommand implements SimpleCommand {

    private final ProxyServer proxy;


    public IpCommand(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length < 1) {
            invocation.source().sendMessage(
                    Component.text("Wrong usage: /ip <username>", NamedTextColor.RED)
            );
            return;
        }

        PlayerIpRepository ipRepository = Goliath.playerIpRepository;
        ProxyPlayerObject playerObject = Goliath.playerRepository.loadPlayerByUsername(args[0]);
        GeoLocation location = ipRepository.getLatestLocation(playerObject.getUuid());
        if (playerObject == null) {
            invocation.source().sendMessage(
                    Component.text("Player has never logged in to the server.", NamedTextColor.RED)
            );
            return;
        }

        String ipAddress = " [NONE] ";

        String locationText = location == null
                ? "Unknown"
                : location.country() + " " + location.city();

        if (proxy.getPlayer(playerObject.getUuid()).isPresent()) {
            ipAddress = proxy.getPlayer(playerObject.getUuid()).get().getRemoteAddress().getAddress().toString();
        }

        invocation.source().sendMessage(
                Component.text("IP: ", NamedTextColor.GRAY)
                        .append(Component.text(ipAddress + " ", NamedTextColor.BLUE)
                                .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, ipAddress + playerObject.getUuid())))
                        .append(Component.text(locationText, NamedTextColor.AQUA))

        );





    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return SimpleCommand.super.suggest(invocation);
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("goliath.command.ip");
    }
}

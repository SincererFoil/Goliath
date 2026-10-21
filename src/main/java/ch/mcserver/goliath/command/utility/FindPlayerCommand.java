package ch.mcserver.goliath.command.utility;

import ch.mcserver.goliath.Goliath;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.Optional;

public class FindPlayerCommand implements SimpleCommand {

    private ProxyServer proxy;

    public FindPlayerCommand(ProxyServer proxy) {
        this.proxy = proxy;
    }


    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length !=1) {
            return;
        }

        String targetName = args[0];

        Optional<Player> optionalPlayer = proxy.getPlayer(targetName);
        if (optionalPlayer.isEmpty()) {
            return;
        }
        Player target = optionalPlayer.get();

        if (target.getCurrentServer().isEmpty()) {
            return;
        }

        ServerConnection server = target.getCurrentServer().get();

        String rawServerName = server.getServerInfo().getName();

        if (!invocation.source().hasPermission("goliath.util.findplayer")) {
            String lower = rawServerName.toLowerCase();

            String serverName = "overworld";

            if (lower.contains("nether")) {
                serverName = "nether";
            } else if (lower.contains("end")) {
                serverName = "end";
            }

            invocation.source().sendMessage(
                    Component.text(targetName + "'s in the ", NamedTextColor.WHITE)
                            .append(Component.text(serverName, NamedTextColor.AQUA))
            );
            return;
        }

        invocation.source().sendMessage(
                Component.text(targetName + "'s in the ", NamedTextColor.WHITE)
                        .append(Component.text(rawServerName, NamedTextColor.AQUA))
        );

    }

    private void findTarget(Player target) {
    }


    @Override
    public List<String> suggest(Invocation invocation) {

        if (invocation.arguments().length == 0) {
            return Goliath.getInstance().getNetworkPlayerRegistry().getOnlinePlayerNames().stream()
                    .toList();
        }

        String input = invocation.arguments()[0].toLowerCase();

        return Goliath.getInstance().getNetworkPlayerRegistry().getOnlinePlayerNames().stream()
                .filter(name -> ((String) name).toLowerCase().startsWith(input)).toList();
    }

}

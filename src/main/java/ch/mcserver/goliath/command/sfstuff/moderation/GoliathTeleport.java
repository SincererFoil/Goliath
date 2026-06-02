package ch.mcserver.goliath.command.sfstuff.moderation;

import ch.mcserver.goliath.Goliath;
import ch.mcserver.goliath.database.repository.mysql.PlayerRepository;
import ch.mcserver.goliath.player.ProxyPlayerObject;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.Optional;

public class GoliathTeleport implements SimpleCommand {

    private final ProxyServer proxy;

    public GoliathTeleport(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public void execute(Invocation invocation) {

        if (!(invocation.source() instanceof Player player)) {
            return;
        }

        String[] args = invocation.arguments();

        if (args.length != 1) {
            player.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
            return;
        }

        PlayerRepository playerRepository = Goliath.playerRepository;
        String targetRawName = args[0];

        if (!playerRepository.existsByUsername(targetRawName)) {
            player.sendMessage(Component.text(
                    "Player " + targetRawName + " does not exist.",
                    NamedTextColor.RED
            ));
            return;
        }

        ProxyPlayerObject playerObject =
                playerRepository.loadPlayerByUsername(targetRawName);

        Optional<Player> optionalOnlineTarget = proxy.getPlayer(targetRawName);

        Optional<RegisteredServer> optionalTargetServer;

        if (optionalOnlineTarget.isPresent()) {

            Player onlineTarget = optionalOnlineTarget.get();

            if (onlineTarget.getCurrentServer().isEmpty()) {
                player.sendMessage(Component.text(
                        "The target player is currently not connected to a server.",
                        NamedTextColor.RED
                ));
                return;
            }

            optionalTargetServer = Optional.of(
                    onlineTarget.getCurrentServer().get().getServer()
            );

        } else {

            optionalTargetServer = proxy.getServer(playerObject.getCurrentServer());
        }

        if (optionalTargetServer.isEmpty()) {
            player.sendMessage(Component.text(
                    "The server you are trying to connect to is currently under maintenance.",
                    NamedTextColor.RED
            ));
            return;
        }

        RegisteredServer targetServer = optionalTargetServer.get();

        if (player.getCurrentServer().isPresent()
                && player.getCurrentServer().get().getServer().equals(targetServer)) {

            Goliath.goliathTeleportMessenger.sendGoliathteleport(
                    player,
                    playerObject.getUuid(),
                    targetServer
            );

            player.sendMessage(Component.text(
                    "Teleported to " + targetRawName + ".",
                    NamedTextColor.GREEN
            ));
            return;
        }

        player.createConnectionRequest(targetServer)
                .connect()
                .thenAccept(result -> {

                    if (!result.isSuccessful()) {
                        player.sendMessage(Component.text(
                                "The server you are trying to connect to is currently under maintenance.",
                                NamedTextColor.RED
                        ));
                        return;
                    }

                    Goliath.goliathTeleportMessenger.sendGoliathteleport(
                            player,
                            playerObject.getUuid(),
                            targetServer
                    );

                    player.sendMessage(Component.text(
                            "Teleported to " + targetRawName + ".",
                            NamedTextColor.GREEN
                    ));
                });
    }

    @Override
    public List<String> suggest(Invocation invocation) {

        if (invocation.arguments().length == 0) {
            return proxy.getAllPlayers()
                    .stream()
                    .map(Player::getUsername)
                    .toList();
        }

        String input = invocation.arguments()[0].toLowerCase();

        return proxy.getAllPlayers()
                .stream()
                .map(Player::getUsername)
                .filter(name -> name.toLowerCase().startsWith(input))
                .toList();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("GoliathCommand.gtp");
    }
}
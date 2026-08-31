package ch.mcserver.goliath.command.admin;

import ch.mcserver.goliath.Goliath;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class GoliathCommand implements SimpleCommand {
    public static HashMap<UUID, Float> playerFlySpeed = new HashMap<>();
    private final ProxyServer proxy;
    private final Object plugin;
    public GoliathCommand(ProxyServer proxy, Object plugin) {
        this.proxy = proxy;
        this.plugin = plugin;
    }


    /**
     * Executes the command for the specified invocation.
     *
     * @param invocation the invocation context
     */
    @Override
    public void execute(Invocation invocation) {
        if (invocation.arguments().length < 1) {
            return;
        }
        String[] args = invocation.arguments();
        String range = args[0];

        switch (range.toLowerCase()) {
            case "move":
                goliathMove(invocation);
                break;
            case "update":
                goliathUpdate(invocation);
                break;
            default:
                return;
        }



    }
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);

    private void goliathMove(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length < 2) {
            invocation.source().sendMessage(Component.text("Usage: /goliath move <server>", NamedTextColor.RED));
            return;
        }

        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(Component.text("Only players can execute this command!", NamedTextColor.RED));
            return;
        }

        String targetServer = args[1];
        Optional<RegisteredServer> server = proxy.getServer(targetServer);
        if (server.isEmpty()) {
            sendMaintenanceMessage(player);
            return;
        }

        if (player.getCurrentServer().isPresent()
                && player.getCurrentServer().get().getServerInfo().getName().equalsIgnoreCase(targetServer)) {
            player.sendMessage(Component.text("You are already connected to this server.", NamedTextColor.RED));
            player.sendActionBar(Component.text("You are already connected to this server.", NamedTextColor.RED));
            return;
        }

        player.createConnectionRequest(server.get())
                .connect()
                .orTimeout(2, TimeUnit.SECONDS)
                .handle((result, throwable) -> {
                    if (throwable != null || !result.isSuccessful()) {
                        player.sendMessage(Component.text("It seems that you are connecting to an area in maintenance,\ntry again in a few minutes.", NamedTextColor.RED));
                        player.sendActionBar(Component.text("Area is currently not available.", NamedTextColor.RED));
                        return null;
                    }

                    player.sendMessage(Component.text("Moved to " + targetServer + ".", NamedTextColor.GREEN));
                    return null;
                });
    }

    private void sendMaintenanceMessage(Player player) {
        player.sendMessage(Component.text(
                "It seems that you are connecting to an area in maintenance,\ntry again in a few minutes.",
                NamedTextColor.RED));
        player.sendActionBar(Component.text("Area is currently not available.", NamedTextColor.RED));
    }


    private void goliathUpdate(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length < 2) {
            invocation.source().sendMessage(Component.text("Wrong usage: /goliath update <servername/all>", NamedTextColor.RED));
        }

        String serverName = args[1];

        if (args[1].equals("all")) {
            Component message = Component.text("We are under maintenance.", NamedTextColor.RED)
                    .appendNewline()
                    .append(Component.text("For more information check the updates channel.", NamedTextColor.WHITE))
                    .appendNewline()
                    .append(Component.text("Join our Discord: ", NamedTextColor.GRAY))
                    .append(Component.text("discord.gg/donutsmp", NamedTextColor.YELLOW));

            for (Player player : proxy.getAllPlayers()) {
                player.disconnect(message);
            }

            try {
                new ProcessBuilder(
                        "setsid",
                        "/bin/bash",
                        "/data/DonutSMP/deploy.sh"
                ).redirectOutput(new File("/data/DonutSMP/deploy.log"))
                        .redirectErrorStream(true)
                        .start();

                invocation.source().sendMessage(Component.text("Deploying network...", NamedTextColor.GREEN));
            } catch (IOException exception) {
                exception.printStackTrace();
                invocation.source().sendMessage(Component.text("Failed to start deployment.", NamedTextColor.RED));
            }

            return;
        }

        try {
            new ProcessBuilder(
                    "/bin/bash",
                    "/data/DonutSMP/update_server.sh",
                    serverName
            ).redirectOutput(new File("/data/DonutSMP/update-server.log"))
                    .redirectErrorStream(true)
                    .start();

            invocation.source().sendMessage(Component.text("Updating " + serverName + "...", NamedTextColor.GREEN));
        } catch (IOException exception) {
            exception.printStackTrace();
            invocation.source().sendMessage(Component.text("Failed to update " + serverName + ".", NamedTextColor.RED));
        }
    }




    /**
     * Provides tab complete suggestions for the specified invocation.
     *
     * @param invocation the invocation context
     * @return the tab complete suggestions
     */
    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 2 && args[0].equalsIgnoreCase("update")) {
            String input = args[1].toLowerCase();

            return Stream.concat(Stream.of("all"), proxy.getAllServers().stream()
                                    .map(server -> server.getServerInfo().getName())
                    ).filter(serverName -> serverName.toLowerCase().startsWith(input.toLowerCase()))
                    .distinct()
                    .sorted()
                    .toList();
        } else if (args.length == 2 && args[0].equalsIgnoreCase("move")) {
            String input = args[1].toLowerCase();

            return proxy.getAllServers().stream()
                            .map(server -> server.getServerInfo().getName())
                    .filter(serverName -> serverName.toLowerCase().startsWith(input.toLowerCase()))
                    .distinct()
                    .sorted()
                    .toList();
        }

        return List.of();
    }


    /**
     * Tests to check if the source has permission to perform the specified invocation.
     *
     * <p>If the method returns {@code false}, the handling is forwarded onto
     * the players current server.
     *
     * @param invocation the invocation context
     * @return {@code true} if the source has permission
     */
    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("goliath.command.use");
    }
}

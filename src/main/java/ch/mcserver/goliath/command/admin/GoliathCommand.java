package ch.mcserver.goliath.command.admin;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
            player.sendMessage(Component.text("It seems that you are connecting to an area in maintenance,\ntry again in a few minutes.", NamedTextColor.RED));
            return;
        }

        if (player.getCurrentServer().isPresent() && player.getCurrentServer().get().getServerInfo().getName().equalsIgnoreCase(targetServer)) {
            player.sendMessage(Component.text("You are already connected to this server.", NamedTextColor.RED));
            player.sendActionBar(Component.text("You are already connected to this server.", NamedTextColor.RED));
            return;
        }
        player.createConnectionRequest(server.get()).connect().thenAccept(result -> {
            if (!result.isSuccessful()) {
                player.sendMessage(Component.text("It seems that you are connecting to an area in maintenance,\ntry again in a few minutes.", NamedTextColor.RED));
                player.sendActionBar(Component.text("Area is currently not available.", NamedTextColor.RED));
                return;
            }
            player.sendMessage(Component.text("Moved to " + targetServer + ".", NamedTextColor.GREEN));
        });
    }


    private void goliathUpdate(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length < 2) {
            Component message = Component.text("We are under maintenance.", NamedTextColor.RED)
                    .appendNewline()
                    .append(Component.text("For more information check updates channel.", NamedTextColor.WHITE))
                    .appendNewline()
                    .append(Component.text("Join us in discord: ", NamedTextColor.GRAY))
                    .append(Component.text("discord.gg/donutsmp", NamedTextColor.YELLOW));

            for (Player player : proxy.getAllPlayers()) {
                player.disconnect(message);
            }

            proxy.getScheduler()
                    .buildTask(plugin, () -> {
                        try {
                            new ProcessBuilder(
                                    "/bin/bash",
                                    "/data/DonutSMP/deploy.sh"
                            ).start();
                        } catch (IOException exception) {
                            exception.printStackTrace();
                        }
                    })
                    .delay(2, TimeUnit.SECONDS)
                    .schedule();

            return;
        }

        String serverName = args[1];

        Optional<RegisteredServer> server = proxy.getServer(serverName);

        if (server.isEmpty()) {
            invocation.source().sendMessage(Component.text(
                    "It seems that this area is currently not available,\ntry again in a few minutes.",
                    NamedTextColor.RED
            ));
            return;
        }

        try {
            new ProcessBuilder(
                    "/bin/bash",
                    "/data/DonutSMP/update_server.sh",
                    serverName
            ).start();

            invocation.source().sendMessage(Component.text(
                    "Updating " + serverName + "...",
                    NamedTextColor.GREEN
            ));

        } catch (IOException exception) {
            exception.printStackTrace();

            invocation.source().sendMessage(Component.text(
                    "Failed to update " + serverName + ".",
                    NamedTextColor.RED
            ));
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

        if (args.length == 2 && args[0].equalsIgnoreCase("move") || args[0].equalsIgnoreCase("update")) {
            String input = args[1].toLowerCase();

            return proxy.getAllServers().stream()
                    .map(registeredServer -> registeredServer.getServerInfo().getName())
                    .filter(serverName -> serverName.toLowerCase().startsWith(input))
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
        return invocation.source().hasPermission("GoliathCommand.use");
    }
}

package ch.mcserver.goliath.command.admin;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GoliathCommand implements SimpleCommand {
    public static HashMap<UUID, Float> playerFlySpeed = new HashMap<>();
    private final ProxyServer proxy;
    public GoliathCommand(ProxyServer proxy) {
        this.proxy = proxy;
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

        if (!(invocation.source() instanceof Player)) {
            invocation.source().sendMessage(Component.text("Only players can execute this command!"));
            return;
        }
        Player player = (Player) invocation.source();

        switch (range.toLowerCase()) {
            case "move":
                goliathMove(player, args);
                break;
            case "update":
                goliathUpdate();
                break;
            default:
                return;
        }



    }
    private void goliathMove(Player player, String[] args) {
        String targetServer = args[1];
        Optional<RegisteredServer> server = proxy.getServer(targetServer);
        if (!server.isPresent()) {
            player.sendMessage(Component.text("It seems that you are connecting to an area in maintenance,\n" +
                    "try again in a few minutes.!", NamedTextColor.RED));
            return;
        }
        player.createConnectionRequest(server.get()).connect().thenAccept(result -> {
                    if (!result.isSuccessful()) {player.sendMessage(Component.text("It seems that you are connecting to an area in maintenance,\ntry again in a few minutes.", NamedTextColor.RED));}});
    }

    private void goliathUpdate() {
        for (Player player : proxy.getAllPlayers()) {
            player.disconnect(Component.text("We are under maintenace.", NamedTextColor.RED).appendNewline().append(Component.text("For more information check updates channel.", NamedTextColor.WHITE)).appendNewline().append(Component.text("Join us in discord: ", NamedTextColor.GRAY)).append(Component.text("discord.gg/donutsmp", NamedTextColor.YELLOW)));
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

        if (args.length == 2 && args[0].equalsIgnoreCase("move")) {
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

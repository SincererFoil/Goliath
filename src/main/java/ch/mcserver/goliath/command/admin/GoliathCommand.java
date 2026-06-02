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
        if (invocation.arguments().length != 2) {
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
                return;
            default:
                return;
        }



    }
    private void goliathMove(Player player, String[] args) {
        String targetServer = args[1];
        Optional<RegisteredServer> server = proxy.getServer(targetServer);
        if (!server.isPresent()) {
            player.sendMessage(Component.text("Server not found!", NamedTextColor.RED));
            return;
        }
        player.createConnectionRequest(server.get()).fireAndForget();

    }




    /**
     * Provides tab complete suggestions for the specified invocation.
     *
     * @param invocation the invocation context
     * @return the tab complete suggestions
     */
    @Override
    public List<String> suggest(Invocation invocation) {
        if (invocation.arguments().length == 2) {
            if (invocation.arguments()[0].equalsIgnoreCase("move")) {
                List<String> suggestions = List.of(
                        "GoliathCommand-dev",
                        "GoliathCommand-devv",
                        "GoliathCommand-ASH-spawn1",
                        "GoliathCommand-ASH-spawn2",
                        "GoliathCommand-ASH-spawn3",
                        "GoliathCommand-ASH-spawn4",
                        "GoliathCommand-ASH-spawn5",
                        "GoliathCommand-ASH-spawn6"
                );
                return suggestions.stream().filter(s -> s.startsWith(invocation.arguments()[1].toLowerCase()))
                        .toList();
            }

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

package ch.mcserver.goliath.command.admin;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class GiveMediaCommand implements SimpleCommand {

    private ProxyServer proxy;

    public GiveMediaCommand(ProxyServer proxy) {
        this.proxy = proxy;
    }

    /**
     * Executes the command for the specified invocation.
     *
     * @param invocation the invocation context
     */
    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player)) {
            return;
        }
        Player player = (Player) invocation.source();
        String[] args = invocation.arguments();

        if (args.length != 1) {
            return;
        }
        String targetPlayer = args[0].toLowerCase();

        proxy.getPlayer(targetPlayer).ifPresentOrElse(target -> {

                    proxy.getCommandManager().executeAsync(
                            proxy.getConsoleCommandSource(),
                            "lpv user " + target.getUsername() + " parent add media"
                    );

                    player.sendMessage(
                            Component.text(
                                    "Gave media rank to " + target.getUsername(),
                                    NamedTextColor.GREEN
                            )
                    );

                }, () -> {

                    player.sendMessage(
                            Component.text(
                                    "Player not found",
                                    NamedTextColor.RED
                            )
                    );

        });

    }

    /**
     * Provides tab complete suggestions for the specified invocation.
     *
     * @param invocation the invocation context
     * @return the tab complete suggestions
     */
    @Override
    public List<String> suggest(Invocation invocation) {
        if (invocation.arguments().length == 1) {
            List<String> suggestions = new ArrayList<>();
            for (Player player : proxy.getAllPlayers()) {
                suggestions.add(player.getUsername().toString().toLowerCase());
            }
            return suggestions.stream().filter(s -> s.startsWith(invocation.arguments()[0].toLowerCase())).toList();
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
        return invocation.source().hasPermission("GoliathCommand.GiveMediaCommand");
    }
}

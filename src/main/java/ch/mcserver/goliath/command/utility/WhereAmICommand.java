package ch.mcserver.goliath.command.utility;

import ch.mcserver.goliath.Goliath;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class WhereAmICommand implements SimpleCommand {
    private final ProxyServer proxy;
    public WhereAmICommand(ProxyServer proxy) {
        this.proxy = proxy;
    }
    /**
     * Executes the command for the specified invocation.
     *
     * @param invocation the invocation context
     */
    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length != 0) {
            return;
        }
        if (!(invocation.source() instanceof Player)) {
            return;
        }
        Player player = (Player) invocation.source();
        if (player.getCurrentServer().isEmpty()) {
            player.disconnect(Component.text("Make a Tiket", NamedTextColor.RED));
            return;
        }
        ServerConnection server = player.getCurrentServer().get();
        String serverName = server.getServerInfo().getName();
        player.sendMessage(
                Component.text("You are currently connected to ", NamedTextColor.GRAY).append(Component.text(serverName, NamedTextColor.AQUA)).append(Component.text(" via proxy ", NamedTextColor.GRAY)).append(Component.text(Goliath.proxyName, NamedTextColor.AQUA)));
    }

    /**
     * Provides tab complete suggestions for the specified invocation.
     *
     * @param invocation the invocation context
     * @return the tab complete suggestions
     */
    @Override
    public List<String> suggest(Invocation invocation) {
        return SimpleCommand.super.suggest(invocation);
    }

    /**
     * Provides tab complete suggestions for the specified invocation.
     *
     * @param invocation the invocation context
     * @return the tab complete suggestions
     */
    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return SimpleCommand.super.suggestAsync(invocation);
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
        return invocation.source().hasPermission("GoliathCommand.whereami");
    }
}

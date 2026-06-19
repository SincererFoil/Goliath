package ch.mcserver.goliath.command.staff;

import ch.mcserver.goliath.player.ProxyPlayerObject;
import ch.mcserver.goliath.pluginmessenger.GmspMessenger;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

import static ch.mcserver.goliath.Goliath.playerRepository;
import static ch.mcserver.goliath.player.ProxyPlayerManager.getPlayer;

public class GmspCommand implements SimpleCommand {

    
    private GmspMessenger messenger;
    private ProxyServer proxy;
    public GmspCommand(GmspMessenger messenger, ProxyServer proxy) {
        this.messenger = messenger;
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
        if (args.length != 0) {
            return;
        }

        ProxyPlayerObject playerObject = getPlayer(player.getUniqueId());
        if (playerObject == null) {
            return;
        }

        if (!playerObject.isSfmode()) {
            player.sendMessage(Component.text(
                    "This command does not exist.",
                    NamedTextColor.RED
            ));
            return;
        }

        if (playerObject.isGmsp()){
            playerObject.setGmsp(false);
            messenger.sendGmsp(player, false);
            playerRepository.save(playerObject);

        } else {
            playerObject.setGmsp(true);
            messenger.sendGmsp(player, true);
            playerRepository.save(playerObject);
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
        return SimpleCommand.super.suggest(invocation);
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
        return SimpleCommand.super.hasPermission(invocation);
    }
}

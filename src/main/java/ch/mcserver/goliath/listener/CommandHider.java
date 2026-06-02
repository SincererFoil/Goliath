package ch.mcserver.goliath.listener;

import ch.mcserver.goliath.player.ProxyPlayerManager;
import ch.mcserver.goliath.player.ProxyPlayerObject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.PlayerAvailableCommandsEvent;
import com.velocitypowered.api.proxy.Player;

import java.util.Set;

public class CommandHider {

    private static final Set<String> STAFF_COMMANDS = Set.of(
            "ban",
            "sus",
            "gtp",
            "invsee",
            "alts",
            "offend",
            "punish",
            "unban",
            "checkban",
            "checkmute",
            "history",
            "spawnstash",
            "spawnplayer",
            "gmsp"
    );

    @Subscribe
    public void onAvailableCommand(PlayerAvailableCommandsEvent event) {

        Player player = event.getPlayer();

        ProxyPlayerObject playerObject =
                ProxyPlayerManager.getPlayer(player.getUniqueId());

        if (playerObject != null && playerObject.isSfmode()) {
            return;
        }

        event.getRootNode().getChildren().removeIf(commandNode -> {
            String commandName = commandNode.getName().toLowerCase();

            if (commandName.contains(":")) {
                commandName = commandName.substring(commandName.indexOf(":") + 1);
            }

            return STAFF_COMMANDS.contains(commandName);
        });
    }
}
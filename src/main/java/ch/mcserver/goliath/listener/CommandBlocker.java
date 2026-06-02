package ch.mcserver.goliath.listener;

import ch.mcserver.goliath.player.ProxyPlayerManager;
import ch.mcserver.goliath.player.ProxyPlayerObject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.proxy.Player;

import java.util.Set;

public class CommandBlocker {

    private static final Set<String> STAFF_COMMANDS = Set.of(
            "ban",
            "sus",
            "gtp",
            "invsee",
            "alts",
            "offend",
            "unban",
            "punish",
            "checkban",
            "checkmute",
            "history",
            "spawnstash",
            "spawnplayer",
            "gmsp"
    );

    @Subscribe
    public void onCommandExecute(CommandExecuteEvent event) {

        if (!(event.getCommandSource() instanceof Player player)) {
            return;
        }

        ProxyPlayerObject playerObject =
                ProxyPlayerManager.getPlayer(player.getUniqueId());

        if (playerObject != null && playerObject.isSfmode()) {
            return;
        }

        String command = event.getCommand().split(" ")[0].toLowerCase();

        if (command.contains(":")) {
            command = command.substring(command.indexOf(":") + 1);
        }

        if (STAFF_COMMANDS.contains(command)) {
            event.setResult(
                    CommandExecuteEvent.CommandResult.forwardToServer()
            );
        }
    }
}
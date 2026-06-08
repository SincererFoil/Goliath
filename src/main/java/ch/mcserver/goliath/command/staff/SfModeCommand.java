package ch.mcserver.goliath.command.staff;

import ch.mcserver.goliath.Goliath;
import ch.mcserver.goliath.player.ProxyPlayerObject;
import ch.mcserver.goliath.pluginmessenger.CommandUpdateMessenger;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.PlayerAvailableCommandsEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static ch.mcserver.goliath.Goliath.playerRepository;
import static ch.mcserver.goliath.player.ProxyPlayerManager.getPlayer;

public class SfModeCommand implements SimpleCommand {

    private final ProxyServer proxy;

    public SfModeCommand(ProxyServer proxy) {
        this.proxy = proxy;
    }

    public static HashMap<UUID, Boolean> isSfmodeMap = new HashMap<>();

    @Override
    public void execute(Invocation invocation) {

        if (!(invocation.source() instanceof Player player)) {
            return;
        }

        if (invocation.arguments().length != 0) {
            return;
        }

        ProxyPlayerObject playerObject =
                getPlayer(player.getUniqueId());

        if (playerObject == null) {

            player.sendMessage(
                    Component.text(
                            "Player data not loaded.",
                            NamedTextColor.RED
                    )
            );

            return;
        }

        boolean enabled = !playerObject.isSfmode();

        playerObject.setSfmode(enabled);

        isSfmodeMap.put(
                player.getUniqueId(),
                enabled
        );

        playerRepository.savePlayerDataOnly(playerObject);

        Component message;

        if (enabled) {

            message = Component.text(
                    "Staff mode is on now, all staff functions will hide on next log in and you won't be able to use any."
            ).color(NamedTextColor.GRAY);
            Goliath.commandUpdateMessenger.sendUpdate(player);
        } else {

            message = Component.text(
                    "Staff mode is off now, on next login everything will be fine."
            ).color(NamedTextColor.GRAY);
            Goliath.commandUpdateMessenger.sendUpdate(player);
        }

        player.sendMessage(message);
        player.sendActionBar(message);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return SimpleCommand.super.suggest(invocation);
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("GoliathCommand.sfmode");
    }

    @Subscribe
    public void onPlayerTabCommand(PlayerAvailableCommandsEvent event) {

    }
}

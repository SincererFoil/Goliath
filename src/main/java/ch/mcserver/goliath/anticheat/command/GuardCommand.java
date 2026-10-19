package ch.mcserver.goliath.anticheat.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

public class GuardCommand implements SimpleCommand {

    public static final Set<UUID> enabledAlerts = ConcurrentHashMap.newKeySet();

    @Override
    public void execute(Invocation invocation) {

        String[] args = invocation.arguments();

        if (args.length == 0) {
            invocation.source().sendMessage(Component.text("Wrong usage: /guard <alert>", NamedTextColor.RED));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "alert" -> toggleAlert(invocation);
        }

    }

    private void toggleAlert(Invocation invocation) {

        if (!(invocation.source() instanceof Player)) {
            invocation.source().sendMessage(Component.text("You must be a player to use this command.", NamedTextColor.RED));
            return;
        }

        Player player = (Player) invocation.source();

        if (enabledAlerts.contains(player.getUniqueId())) {
            player.sendMessage(Component.text("Alerts off", NamedTextColor.GRAY));
            enabledAlerts.remove(player.getUniqueId());
        } else  {
            enabledAlerts.add(player.getUniqueId());
            player.sendMessage(Component.text("Alerts on", NamedTextColor.GRAY));
        }

    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return SimpleCommand.super.suggest(invocation);
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("goliath.guard.command.use");
    }
}

package ch.mcserver.goliath.anticheat.alert;

import ch.mcserver.goliath.anticheat.AnticheatFlagMessage;
import ch.mcserver.goliath.anticheat.command.GuardCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class AnticheatAlert {

    private final ProxyServer proxy;

    public AnticheatAlert(ProxyServer proxy) {
        this.proxy = proxy;
    }

    public void sendAlert(AnticheatFlagMessage flagMessage) {
        TextComponent message = Component.text("[", NamedTextColor.GRAY)
                .append(Component.text("SUS", NamedTextColor.AQUA))
                .append(Component.text("] ", NamedTextColor.GRAY))
                .append(Component.text(flagMessage.playerName(), NamedTextColor.GRAY)
                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/gtp " + flagMessage.playerName()))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to teleport", NamedTextColor.GRAY))))
                .append(Component.text(" FAILED ", NamedTextColor.AQUA))
                .append(Component.text(flagMessage.checkName(), NamedTextColor.GRAY))
                .append(Component.text(" (", NamedTextColor.GRAY))
                .append(Component.text(flagMessage.violations(), NamedTextColor.GRAY))
                .append(Component.text("x )", NamedTextColor.GRAY))
                .append(Component.text(" | SERVER ", NamedTextColor.GRAY))
                .append(Component.text(flagMessage.serverName(), NamedTextColor.WHITE)
                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/goliath move " + flagMessage.serverName()))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to switch server", NamedTextColor.GRAY))))
                .append(Component.text(" | BUFFER ", NamedTextColor.GRAY))
                .append(Component.text(flagMessage.details(), NamedTextColor.WHITE));

        GuardCommand.enabledAlerts.forEach(uuid -> {
            proxy.getPlayer(uuid).ifPresent(player -> {player.sendMessage(message);});
        });
    }
}
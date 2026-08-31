package ch.mcserver.goliath.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class BackendKickListener {

    private static final String MAINTENANCE_MARKER = "[GOLIATH_MAINTENANCE]";

    @Subscribe
    public void onBackendKick(KickedFromServerEvent event) {
        Component originalReason = event.getServerKickReason()
                .orElse(Component.text("Disconnected"));

        String plainReason = PlainTextComponentSerializer.plainText()
                .serialize(originalReason);

        String lowerReason = plainReason.toLowerCase();

        boolean kickedFromCurrentServer = event.getPlayer().getCurrentServer()
                .map(ServerConnection::getServer)
                .map(server -> server.equals(event.getServer()))
                .orElse(false);

        Component kickMessage;

        if (plainReason.contains(MAINTENANCE_MARKER)) {
            kickMessage = Component.text("This area is currently under maintenance.", NamedTextColor.RED)
                    .appendNewline()
                    .appendNewline()
                    .append(Component.text("Please try again in a few minutes.", NamedTextColor.WHITE));
        } else if (lowerReason.contains("outdated server")
                || lowerReason.contains("outdated client")
                || lowerReason.contains("incompatible version")) {
            kickMessage = Component.text("Unsupported Minecraft version.", NamedTextColor.RED)
                    .appendNewline()
                    .appendNewline()
                    .append(Component.text("Please connect using a supported version.", NamedTextColor.WHITE));
        } else if (kickedFromCurrentServer) {
            kickMessage = originalReason;
        } else {
            kickMessage = Component.text("We don't know what happened here.", NamedTextColor.RED)
                    .appendNewline()
                    .appendNewline()
                    .append(Component.text("Please create a support ticket on our Discord.", NamedTextColor.WHITE))
                    .appendNewline()
                    .append(Component.text("discord.gg/donutsmp", NamedTextColor.YELLOW));
        }

        event.setResult(KickedFromServerEvent.DisconnectPlayer.create(kickMessage));
    }
}
package ch.mcserver.goliath.pluginmessenger;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

public class GmspMessenger {

    public static final MinecraftChannelIdentifier CHANNEL =
            MinecraftChannelIdentifier.from("goliath:gmsp");

    private final ProxyServer proxy;

    public GmspMessenger(ProxyServer proxy) {
        this.proxy = proxy;
        this.proxy.getChannelRegistrar().register(CHANNEL);
    }

    public void sendGmsp(Player player, boolean enabled) {

        player.getCurrentServer().ifPresent(serverConnection -> {

            ByteArrayDataOutput out = ByteStreams.newDataOutput();

            out.writeUTF("GMSP");
            out.writeUTF(player.getUniqueId().toString());
            out.writeBoolean(enabled);

            serverConnection.sendPluginMessage(
                    CHANNEL,
                    out.toByteArray()
            );
        });
    }
}

package ch.mcserver.goliath.pluginmessenger;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

public class CommandUpdateMessenger {

    public static final MinecraftChannelIdentifier CHANNEL =
            MinecraftChannelIdentifier.from("goliath:updatecommands");

    private final ProxyServer proxy;

    public CommandUpdateMessenger(ProxyServer proxy) {
        this.proxy = proxy;
        this.proxy.getChannelRegistrar().register(CHANNEL);
    }

    public void sendUpdate(Player player) {
        player.getCurrentServer().ifPresent(serverConnection -> {

            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF(player.getUniqueId().toString());

            serverConnection.sendPluginMessage(
                    CHANNEL,
                    out.toByteArray()
            );
        });
    }
}
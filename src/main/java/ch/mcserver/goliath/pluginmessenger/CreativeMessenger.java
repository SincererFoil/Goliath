package ch.mcserver.goliath.pluginmessenger;

import ch.mcserver.goliath.player.ProxyPlayerObject;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import java.util.UUID;

import static ch.mcserver.goliath.Goliath.playerRepository;
import static ch.mcserver.goliath.player.ProxyPlayerManager.getPlayer;

public class CreativeMessenger {

    public static final MinecraftChannelIdentifier CHANNEL =
            MinecraftChannelIdentifier.from("goliath:creative");

    private final ProxyServer proxy;

    public CreativeMessenger(ProxyServer proxy) {
        this.proxy = proxy;
        proxy.getChannelRegistrar().register(CHANNEL);
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(CHANNEL)) return;

        event.setResult(PluginMessageEvent.ForwardResult.handled());

        ByteArrayDataInput input = ByteStreams.newDataInput(event.getData());
        UUID uuid = UUID.fromString(input.readUTF());
        boolean enabled = input.readBoolean();

        ProxyPlayerObject playerObject = getPlayer(uuid);
        if (playerObject == null) return;

        playerObject.setCreative(enabled);
        playerRepository.savePlayerDataOnly(playerObject);
    }

    public void sendCreative(Player player, boolean enabled) {
        player.getCurrentServer().ifPresent(serverConnection -> {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF(player.getUniqueId().toString());
            out.writeBoolean(enabled);
            serverConnection.sendPluginMessage(CHANNEL, out.toByteArray());
        });
    }
}

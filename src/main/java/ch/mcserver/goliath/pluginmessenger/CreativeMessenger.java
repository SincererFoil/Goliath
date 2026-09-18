package ch.mcserver.goliath.pluginmessenger;

import ch.mcserver.goliath.Goliath;
import ch.mcserver.goliath.player.ProxyPlayerObject;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
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

        if (!(event.getSource() instanceof ServerConnection serverConnection)) {
            if (event.getSource() instanceof Player player) {
                Goliath.LOGGER.warn(
                        "Player {} ({}) tried to send protected plugin message {}",
                        player.getUsername(),
                        player.getUniqueId(),
                        CHANNEL.getId()
                );
            }

            return;
        }

        Player player = serverConnection.getPlayer();

        try {
            ByteArrayDataInput input = ByteStreams.newDataInput(event.getData());

            UUID uuid = UUID.fromString(input.readUTF());
            boolean enabled = input.readBoolean();

            if (!player.getUniqueId().equals(uuid)) {
                Goliath.LOGGER.warn(
                        "Rejected creative payload UUID mismatch from {}: expected {}, received {}",
                        player.getUsername(),
                        player.getUniqueId(),
                        uuid
                );

                return;
            }

            ProxyPlayerObject playerObject = getPlayer(uuid);

            if (playerObject == null) return;

            playerObject.setCreative(enabled);
            playerRepository.savePlayerDataOnly(playerObject);

        } catch (RuntimeException exception) {
            Goliath.LOGGER.warn(
                    "Rejected malformed creative payload from {} ({})",
                    player.getUsername(),
                    player.getUniqueId(),
                    exception
            );
        }
    }

    public void sendCreative(Player player, boolean enabled) {
        player.getCurrentServer().ifPresent(serverConnection -> {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();

            out.writeUTF(player.getUniqueId().toString());
            out.writeBoolean(enabled);

            serverConnection.sendPluginMessage(
                    CHANNEL,
                    out.toByteArray()
            );
        });
    }
}
package ch.mcserver.goliath.history;

import ch.mcserver.goliath.Goliath;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import java.util.UUID;

public class SnapshotRequestManager {

    public static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from("goliath:history");

    public final ProxyServer proxy;

    public SnapshotRequestManager(ProxyServer proxy) {
        this.proxy = proxy;
        this.proxy.getChannelRegistrar().register(CHANNEL);
    }


    public void requestSnapshot(UUID playerUUID, String historyId, String type) {
        if (playerUUID == null || historyId == null || type == null) {
            Goliath.LOGGER.warn("[Goliath] SnapshotRequest failed. PlayerUUID: " + playerUUID + " HistoryId: " + historyId);
            return;
        }

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("HISTORY");
        out.writeUTF(playerUUID.toString());
        out.writeUTF(historyId);
        out.writeUTF(type);

        proxy.getPlayer(playerUUID).ifPresentOrElse(player -> player.getCurrentServer().ifPresentOrElse(server -> {server.sendPluginMessage(CHANNEL, out.toByteArray());}, () -> Goliath.LOGGER.warn("[Goliath] SnapshotRequest failed. Player has no current server. PlayerUUID: " + playerUUID)), () -> Goliath.LOGGER.warn("[Goliath] SnapshotRequest failed. Player is offline. PlayerUUID: " + playerUUID));

    }

}

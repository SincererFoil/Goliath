package ch.mcserver.goliath.pluginmessenger;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import java.util.Optional;
import java.util.UUID;

public class SusInspectionMessenger {

    public static final MinecraftChannelIdentifier CHANNEL =
            MinecraftChannelIdentifier.from("goliath:anticheat:sus");

    private final ProxyServer proxy;

    public SusInspectionMessenger(ProxyServer proxy) {
        this.proxy = proxy;
        proxy.getChannelRegistrar().register(CHANNEL);
    }

    public boolean sendOpenSusMessage(UUID staffUuid, String suspectsJson) {
        if (staffUuid == null || suspectsJson == null) {
            return false;
        }

        Optional<ServerConnection> connection = getConnection(staffUuid);

        if (connection.isEmpty()) {
            return false;
        }

        ByteArrayDataOutput output = ByteStreams.newDataOutput();

        output.writeUTF("OPEN");
        output.writeUTF(staffUuid.toString());
        output.writeUTF(suspectsJson);

        return connection.get().sendPluginMessage(
                CHANNEL,
                output.toByteArray()
        );
    }

    public boolean sendStartSusInspectionMessage(
            UUID staffUuid,
            UUID targetUuid
    ) {
        if (staffUuid == null || targetUuid == null) {
            return false;
        }

        Optional<Player> staffOptional = proxy.getPlayer(staffUuid);
        Optional<Player> targetOptional = proxy.getPlayer(targetUuid);

        if (staffOptional.isEmpty() || targetOptional.isEmpty()) {
            return false;
        }

        Optional<ServerConnection> staffConnection =
                staffOptional.get().getCurrentServer();

        Optional<ServerConnection> targetConnection =
                targetOptional.get().getCurrentServer();

        if (staffConnection.isEmpty() || targetConnection.isEmpty()) {
            return false;
        }

        String staffServer = staffConnection.get()
                .getServerInfo()
                .getName();

        String targetServer = targetConnection.get()
                .getServerInfo()
                .getName();

        if (!staffServer.equals(targetServer)) {
            return false;
        }

        ByteArrayDataOutput output = ByteStreams.newDataOutput();

        output.writeUTF("START_INSPECTION");
        output.writeUTF(staffUuid.toString());
        output.writeUTF(targetUuid.toString());

        return staffConnection.get().sendPluginMessage(
                CHANNEL,
                output.toByteArray()
        );
    }

    public boolean sendStopSusInspectionMessage(UUID staffUuid) {
        if (staffUuid == null) {
            return false;
        }

        Optional<ServerConnection> connection = getConnection(staffUuid);

        if (connection.isEmpty()) {
            return false;
        }

        ByteArrayDataOutput output = ByteStreams.newDataOutput();

        output.writeUTF("STOP_INSPECTION");
        output.writeUTF(staffUuid.toString());

        return connection.get().sendPluginMessage(
                CHANNEL,
                output.toByteArray()
        );
    }

    private Optional<ServerConnection> getConnection(UUID playerUuid) {
        return proxy.getPlayer(playerUuid)
                .flatMap(Player::getCurrentServer);
    }
}
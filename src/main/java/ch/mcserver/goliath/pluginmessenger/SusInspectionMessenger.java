package ch.mcserver.goliath.pluginmessenger;

import ch.mcserver.goliath.player.ProxyPlayerObject;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.UUID;


public class SusInspectionMessenger {

    public static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from("goliath:anticheat:sus");

    private final ProxyServer proxy;

    public SusInspectionMessenger(ProxyServer proxy) {
        this.proxy = proxy;
        this.proxy.getChannelRegistrar().register(CHANNEL);
    }

    public void sendOpenSusMessage(UUID staffUuid) {

       if (staffUuid == null) {
           return;
       }

       RegisteredServer registeredServer = proxy.getPlayer(staffUuid).orElseThrow().getCurrentServer().orElseThrow().getServer();

        ByteArrayDataOutput out = ByteStreams.newDataOutput();

        out.writeUTF("OPEN");
        out.writeUTF(staffUuid.toString());

        registeredServer.sendPluginMessage(
                CHANNEL,
                out.toByteArray()
        );
    }

    public void sendStartSusInspectionMessage(UUID staffUuid, UUID targetUuid) {

        RegisteredServer registeredServer = proxy.getPlayer(targetUuid).orElseThrow().getCurrentServer().orElseThrow().getServer();

        ByteArrayDataOutput out = ByteStreams.newDataOutput();

        out.writeUTF("START_INSPECTION");
        out.writeUTF(staffUuid.toString());
        out.writeUTF(targetUuid.toString());

        registeredServer.sendPluginMessage(
                CHANNEL,
                out.toByteArray()
        );

    }

    public void sendStopSusInspectionMessage(UUID staffUuid, UUID targetUuid) {

        RegisteredServer registeredServer = proxy.getPlayer(targetUuid).orElseThrow().getCurrentServer().orElseThrow().getServer();

        ByteArrayDataOutput out = ByteStreams.newDataOutput();

        out.writeUTF("STOP_INSPECTION");
        out.writeUTF(staffUuid.toString());
        out.writeUTF(targetUuid.toString());
        registeredServer.sendPluginMessage(
                CHANNEL,
                out.toByteArray()
        );

    }


}
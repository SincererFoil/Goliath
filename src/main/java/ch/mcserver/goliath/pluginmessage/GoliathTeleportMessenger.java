package ch.mcserver.goliath.pluginmessage;

import ch.mcserver.goliath.player.ProxyPlayerObject;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.UUID;

import static ch.mcserver.goliath.player.ProxyPlayerManager.getPlayer;

public class GoliathTeleportMessenger {

    public static final MinecraftChannelIdentifier CHANNEL =
            MinecraftChannelIdentifier.from("goliath:gtp");

    private final ProxyServer proxy;

    public GoliathTeleportMessenger(ProxyServer proxy) {
        this.proxy = proxy;
        this.proxy.getChannelRegistrar().register(CHANNEL);
    }

    public void sendGoliathteleport(Player staff, UUID target, RegisteredServer registeredServer) {

        if (staff == null || target == null || registeredServer == null) {
            return;
        }

        ProxyPlayerObject staffObject = getPlayer(staff.getUniqueId());

        boolean gmspEnabled = staffObject != null && staffObject.isGmsp();

        ByteArrayDataOutput out = ByteStreams.newDataOutput();

        out.writeUTF("GTP");
        out.writeUTF(staff.getUniqueId().toString());
        out.writeUTF(target.toString());
        out.writeBoolean(gmspEnabled);

        registeredServer.sendPluginMessage(
                CHANNEL,
                out.toByteArray()
        );
    }
}
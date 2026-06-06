package ch.mcserver.goliath.listener;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ServerSwitchListener {

    @Subscribe
    public void onPreConnect(ServerPreConnectEvent event) {

        if (event.getOriginalServer() == null) {
            event.setResult(ServerPreConnectEvent.ServerResult.denied());

            event.getPlayer().sendMessage(Component.text("It seems that you are connecting to an area in maintenance, try again in a few minutes.",NamedTextColor.RED));
        }
    }
}
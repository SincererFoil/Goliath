package ch.mcserver.goliath.anticheat.listener;

import ch.mcserver.goliath.Goliath;
import ch.mcserver.goliath.anticheat.module.session.SessionManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;

public class AnticheatListener {

    private final SessionManager sessionManager;

    public AnticheatListener(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Subscribe
    public void onPlayerLogin(LoginEvent event) {
        sessionManager.onConnect(event.getPlayer());

    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        if (event.getLoginStatus() == DisconnectEvent.LoginStatus.SUCCESSFUL_LOGIN) {

            sessionManager.onDisconnect(event.getPlayer().getUniqueId());
        }
    }

}

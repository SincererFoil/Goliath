package ch.mcserver.goliath.player;

import ch.mcserver.goliath.player.session.DisconnectType;
import ch.mcserver.goliath.player.session.ProxyPlayerSessionManager;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;


public class ProxyPlayerConnectionListener {

    private final ProxyPlayerSessionManager sessionManager;

    public ProxyPlayerConnectionListener(ProxyPlayerSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Subscribe
    public EventTask onPlayerPostLogin(PostLoginEvent event) {
        return EventTask.async(() ->
                ProxyPlayerManager.loadPlayer(event.getPlayer())
        );
    }

    @Subscribe
    public EventTask onPlayerDisconnect(DisconnectEvent event) {

        DisconnectEvent.LoginStatus status = event.getLoginStatus();

        boolean shouldCleanup = status == DisconnectEvent.LoginStatus.SUCCESSFUL_LOGIN || status == DisconnectEvent.LoginStatus.CANCELLED_BY_USER_BEFORE_COMPLETE || status == DisconnectEvent.LoginStatus.PRE_SERVER_JOIN;


        if (!shouldCleanup) {
            return null;
        }

        String sessionId = sessionManager.getSession(event.getPlayer().getUniqueId());

        if (sessionId == null) {
            return null;
        }


        return EventTask.async(() -> {
            ProxyPlayerManager.unloadPlayer(event.getPlayer());
            sessionManager.removeSession(event.getPlayer().getUniqueId(), sessionId);
        });
    }

    @Subscribe
    public void onPlayerLoginEvent(LoginEvent event) {
        if (!sessionManager.createSession(event.getPlayer().getUniqueId())) {

            String sessionId = sessionManager.getSession(event.getPlayer().getUniqueId());
            String proxyId = sessionId != null ? sessionManager.getProxyId(sessionId) : null;
            String displayedProxy = proxyId != null ? proxyId : "unknown";

            Component proxyRequestJoinCacheLoginMessage = (Component.text("You are already online", NamedTextColor.RED)
                    .appendNewline()
                    .append(Component.text("You are connected to proxy: ", NamedTextColor.GRAY))
                    .append(Component.text(displayedProxy, NamedTextColor.WHITE))
                    .appendNewline()
                    .append(Component.text("Connect reason: ", NamedTextColor.GRAY))
                    .append(Component.text("proxy request-join-cache login", NamedTextColor.WHITE)));

            event.setResult(ResultedEvent.ComponentResult.denied(proxyRequestJoinCacheLoginMessage));

        }
    }

}

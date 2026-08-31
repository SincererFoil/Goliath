package ch.mcserver.goliath.listener;

import ch.mcserver.goliath.database.mysql.repository.PlayerIpRepository;
import ch.mcserver.goliath.player.alts.GeoIpService;
import ch.mcserver.goliath.player.alts.GeoLocation;
import ch.mcserver.goliath.player.alts.IpHasher;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;

import java.net.InetAddress;

public class PlayerIpConnectListener {

    private final PlayerIpRepository repository;
    private final GeoIpService geoIpService;
    private final IpHasher ipHasher;

    public PlayerIpConnectListener(
            PlayerIpRepository repository,
            GeoIpService geoIpService,
            IpHasher ipHasher
    ) {
        this.repository = repository;
        this.geoIpService = geoIpService;
        this.ipHasher = ipHasher;
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        InetAddress address = player.getRemoteAddress().getAddress();

        String ipHash = ipHasher.hash(address.getHostAddress());
        GeoLocation location = geoIpService.lookup(address);

        repository.save(player.getUniqueId(), ipHash, location);
    }
}
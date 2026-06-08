package ch.mcserver.goliath;

import ch.mcserver.goliath.command.admin.GoliathCommand;
import ch.mcserver.goliath.command.admin.GiveMediaCommand;
import ch.mcserver.goliath.command.moderation.*;
import ch.mcserver.goliath.command.staff.GmspCommand;
import ch.mcserver.goliath.command.staff.SfModeCommand;
import ch.mcserver.goliath.command.utility.FindPlayerCommand;
import ch.mcserver.goliath.command.utility.WhereAmICommand;
import ch.mcserver.goliath.database.mongodb.MongoDBManager;
import ch.mcserver.goliath.database.mysql.MySQLManager;
import ch.mcserver.goliath.database.mysql.repository.PlayerRepository;
import ch.mcserver.goliath.database.mongodb.repository.HistoryEventRepository;
import ch.mcserver.goliath.history.HistoryEventListener;
import ch.mcserver.goliath.history.HistroyLogTypes;
import ch.mcserver.goliath.listener.CommandBlocker;
import ch.mcserver.goliath.listener.CommandHider;
import ch.mcserver.goliath.listener.GmspServerSwitchListener;
import ch.mcserver.goliath.player.ProxyPlayerManager;
import ch.mcserver.goliath.pluginmessenger.CommandUpdateMessenger;
import ch.mcserver.goliath.pluginmessenger.GmspMessenger;
import ch.mcserver.goliath.pluginmessenger.GoliathTeleportMessenger;
import ch.mcserver.goliath.history.SnapshotRequestManager;
import ch.mcserver.goliath.listener.PunishmentConnectListener;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Plugin(
        id = "goliath",
        name = "Goliath",
        version = "1.0",
        authors = {"SincererFoil"}
)
public class Goliath {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;

    public static String proxyName = "goliath-EU-proxy1";

    public static final Map<UUID, Float> flySpeeds = new HashMap<>();

    public static MySQLManager mySQLManager;
    public static MongoDBManager mongoDBManager;

    public static GoliathTeleportMessenger goliathTeleportMessenger;
    public static CommandUpdateMessenger commandUpdateMessenger;

    public static PlayerRepository playerRepository;

    public static ConfigurationNode config;

    public static final Logger LOGGER = LoggerFactory.getLogger(Goliath.class);

    @Inject
    public Goliath(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {

        logger.info("Goliath Core enabled.");

        loadConfig();

        mySQLManager = new MySQLManager();
        mySQLManager.connect();

        java.util.logging.Logger.getLogger("org.mongodb.driver").setLevel(java.util.logging.Level.WARNING);
        java.util.logging.Logger.getLogger("org.mongodb.driver.client").setLevel(java.util.logging.Level.WARNING);
        java.util.logging.Logger.getLogger("org.mongodb.driver.cluster").setLevel(java.util.logging.Level.WARNING);

        mongoDBManager = new MongoDBManager();
        mongoDBManager.connect();

        playerRepository = new PlayerRepository(mySQLManager);
        goliathTeleportMessenger = new GoliathTeleportMessenger(proxy);
        commandUpdateMessenger = new CommandUpdateMessenger(proxy);

        SnapshotRequestManager snapshotRequestManager = new SnapshotRequestManager(proxy);
        HistoryEventRepository historyRepository = new HistoryEventRepository(mongoDBManager.getCollection("history_events"));
        HistroyLogTypes historyLogTypes = new HistroyLogTypes(proxy, snapshotRequestManager, historyRepository);

        GmspMessenger gmspMessenger = new GmspMessenger(proxy);

        proxy.getEventManager().register(this, new ProxyPlayerManager());
        proxy.getEventManager().register(this, new CommandHider());
        proxy.getEventManager().register(this, new CommandBlocker());
        proxy.getEventManager().register(this, new PunishmentConnectListener());
        proxy.getEventManager().register(this, new GmspServerSwitchListener(proxy, this, gmspMessenger));
        proxy.getEventManager().register(this, new HistoryEventListener(historyLogTypes));

        proxy.getCommandManager().register(
                proxy.getCommandManager().metaBuilder("goliath").plugin(this).build(),
                new GoliathCommand(proxy)
        );

        proxy.getCommandManager().register(
                proxy.getCommandManager().metaBuilder("whereami").plugin(this).build(),
                new WhereAmICommand(proxy)
        );

        proxy.getCommandManager().register(
                proxy.getCommandManager().metaBuilder("giveMedia").aliases("goliath:giveMedia").plugin(this).build(),
                new GiveMediaCommand(proxy)
        );

        proxy.getCommandManager().register(
                proxy.getCommandManager().metaBuilder("sfmode").plugin(this).build(),
                new SfModeCommand(proxy)
        );

        proxy.getCommandManager().register(
                proxy.getCommandManager().metaBuilder("gmsp").plugin(this).build(),
                new GmspCommand(gmspMessenger, proxy)
        );

        proxy.getCommandManager().register(
                proxy.getCommandManager().metaBuilder("offend").aliases("punish").plugin(this).build(),
                new OffendCommand(proxy)
        );

        proxy.getCommandManager().register(
                proxy.getCommandManager().metaBuilder("checkban").aliases("goliath:checkban").plugin(this).build(),
                new CheckBanCommand()
        );

        proxy.getCommandManager().register(
                proxy.getCommandManager().metaBuilder("ban").aliases("goliath:ban").plugin(this).build(),
                new BanCommand(proxy)
        );

        proxy.getCommandManager().register(
                proxy.getCommandManager().metaBuilder("unban").aliases("goliath:unban").plugin(this).build(),
                new UnbanCommand()
        );

        proxy.getCommandManager().register(
                proxy.getCommandManager().metaBuilder("gtp").aliases("goliath:gtp").plugin(this).build(),
                new GoliathTeleportCommand(proxy)
        );

        proxy.getCommandManager().register(
                proxy.getCommandManager().metaBuilder("fp").aliases("findplayer").plugin(this).build(),
                new FindPlayerCommand(proxy)
        );
    }

    private void loadConfig() {
        try {
            Files.createDirectories(dataDirectory);

            Path configPath = dataDirectory.resolve("config.yml");

            if (!Files.exists(configPath)) {
                try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                    if (inputStream == null) {
                        throw new IOException("config.yml not found in resources.");
                    }

                    Files.copy(inputStream, configPath);
                }
            }

            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                    .path(configPath)
                    .build();

            config = loader.load();

            logger.info("[Goliath] Config loaded.");

        } catch (Exception exception) {
            logger.error("[Goliath] Failed to load config.", exception);
        }
    }

    public GoliathTeleportMessenger getGoliathTeleportMessenger() {
        return goliathTeleportMessenger;
    }
}
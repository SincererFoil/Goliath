package ch.mcserver.goliath.command.moderation.punish;

import ch.mcserver.goliath.Goliath;
import ch.mcserver.goliath.database.redis.RedisManager;
import com.google.gson.Gson;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PunishRedisService {

    private static final String CHANNEL = "goliath:network:punishments";

    private final RedisManager redisManager;

    private final ProxyServer proxy;

    private Gson gson = new Gson();

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private volatile boolean running = false;

    private volatile Jedis activeConnection;

    private volatile JedisPubSub activeSubscriber;

    public PunishRedisService(ProxyServer proxy, RedisManager redisManager) {
        this.redisManager = redisManager;
        this.proxy = proxy;
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        executor.submit(this::subscribeLoop);
    }

    public void stop() {
        running = false;

        JedisPubSub subscriber = activeSubscriber;
        if (subscriber != null) {
            subscriber.unsubscribe();
        }

        Jedis jedis = activeConnection;

        if (jedis != null) {
            jedis.close();
        }

        executor.shutdownNow();
    }

    private void subscribeLoop() {
        while (running) {

            try (Jedis jedis = redisManager.getConnection()) {

                activeConnection = jedis;

                JedisPubSub subscriber = new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        try {
                            handlePunishEvent(message);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };

                activeSubscriber = subscriber;

                jedis.subscribe(subscriber, CHANNEL);


            } catch (Exception e) {
                if (running) {
                    e.printStackTrace();
                }
            } finally {
                activeConnection = null;
                activeSubscriber = null;
            }


            if (!running) break;

            try {
                Thread.sleep(3000);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            }

        }
    }

    private void handlePunishEvent(String json) {

        try {
            PunishEvent event = gson.fromJson(json, PunishEvent.class);

            if (event.targetUuid() == null || event.bannedBy() == null || event.reason() == null || event.banId() == null || event.issuedAt() == null) return;

            if (proxy.getPlayer(event.targetUuid()).isEmpty()) return;

            if (event.isPermanent()) {
                executePermanentPunishment(event);
            } else {
                if (event.expiresAt() == null) return;

                executeTemporaryPunishment(event);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void executeTemporaryPunishment(PunishEvent event) {
        Optional<Player> optionalTarget = proxy.getPlayer(event.targetUuid());

        if (optionalTarget.isEmpty()) return;

        Player target = optionalTarget.get();


        Component banMessage = event.reason().contains("&") ? LegacyComponentSerializer.legacyAmpersand().deserialize(event.reason()) : Component.text(event.reason(), NamedTextColor.RED);

        Instant expiresAt = Instant.ofEpochMilli(event.expiresAt());

        Duration remaining = Duration.between(Instant.now(), expiresAt);

        long totalMinutes = Math.max(0, remaining.toMinutes());
        long days = totalMinutes / (24 * 60);
        long hours = (totalMinutes % (24 * 60)) / 60;
        long minutes = totalMinutes % 60;

        String formatted = days + " Days " + hours + " Hours " + minutes + " Minutes";

        target.disconnect(
                banMessage
                        .appendNewline()
                        .appendNewline()
                        .append(Component.text("Time Left: ", NamedTextColor.GRAY))
                        .append(Component.text(formatted, NamedTextColor.WHITE))
                        .appendNewline()
                        .appendNewline()
                        .append(Component.text("Ban ID: ", NamedTextColor.GRAY))
                        .append(Component.text(event.banId(), NamedTextColor.WHITE))
                        .appendNewline()
                        .appendNewline()
                        .append(Component.text("You may be able to appeal this ban on", NamedTextColor.GRAY))
                        .appendNewline()
                        .append(Component.text("discord.gg/mcserver", NamedTextColor.WHITE))
        );
    }

    private void executePermanentPunishment(PunishEvent event) {
        Optional<Player> optionalTarget = proxy.getPlayer(event.targetUuid());

        if (optionalTarget.isEmpty()) return;

        Player target = optionalTarget.get();

        String formattedDate = Instant.ofEpochMilli(event.issuedAt()).atZone(ZoneId.of("Europe/Zurich")).format(formatter);

        Component banMessage = event.reason().contains("&") ? LegacyComponentSerializer.legacyAmpersand().deserialize(event.reason()) : Component.text(event.reason(), NamedTextColor.RED);

        target.disconnect(
                banMessage
                        .appendNewline()
                        .appendSpace()
                        .appendNewline()
                        .append(Component.text("Date: ", NamedTextColor.GRAY))
                        .append(Component.text(formattedDate, NamedTextColor.WHITE))
                        .appendNewline()
                        .appendSpace()
                        .appendNewline()
                        .append(Component.text("Ban ID: ", NamedTextColor.GRAY))
                        .append(Component.text(event.banId(), NamedTextColor.WHITE))
                        .appendNewline()
                        .appendSpace()
                        .appendNewline()
                        .append(Component.text("You may be able to appeal this ban on", NamedTextColor.GRAY))
                        .appendNewline()
                        .append(Component.text("discord.gg/mcserver", NamedTextColor.WHITE))
        );
    }

    public void publish(PunishEvent event) {

        String json = gson.toJson(event);

      try (Jedis jedis = redisManager.getConnection()) {
          jedis.publish(CHANNEL, json);
      } catch (Exception e) {
          e.printStackTrace();
      }

    }




}

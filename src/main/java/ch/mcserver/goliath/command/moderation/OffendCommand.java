package ch.mcserver.goliath.command.moderation;

import ch.mcserver.goliath.Goliath;
import ch.mcserver.goliath.database.mysql.repository.PlayerRepository;
import ch.mcserver.goliath.player.ProxyPlayerObject;
import ch.mcserver.goliath.player.punishments.PlayerPunishment;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class OffendCommand implements SimpleCommand {

    private final ProxyServer proxy;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public OffendCommand(ProxyServer proxy) {
        this.proxy = proxy;
    }

    public static String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();

        return days + " Days " + hours + " Hours " + minutes + " Minutes";
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        String staffName = checkOffendSource(invocation);

        if (args.length < 2) {
            invocation.source().sendMessage(Component.text(
                    "Wrong Usage: /offend <player> <reason> [note: <staff-note>]",
                    NamedTextColor.RED
            ));
            return;
        }

        String targetName = args[0];
        String staffNote = null;

        if (args.length >= 3) {
            String noteInput = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            int noteIndex = noteInput.toLowerCase().indexOf("note:");

            if (noteIndex >= 0) {
                staffNote = noteInput.substring(noteIndex + "note:".length()).trim();

                if (staffNote.isEmpty()) {
                    staffNote = null;
                }
            }
        }

        PlayerRepository playerRepository = Goliath.playerRepository;

        if (!playerRepository.existsByUsername(targetName)) {
            invocation.source().sendMessage(Component.text(
                    "This player has never logged in to the server.",
                    NamedTextColor.RED
            ));
            return;
        }

        ProxyPlayerObject targetObject = playerRepository.loadPlayerByUsername(targetName);

        if (targetObject.getPunishments() == null) {
            targetObject.setPunishments(new ArrayList<>());
        }

        String rawReason = args[1].toLowerCase();

        if (!durations.containsKey(rawReason)) {
            invocation.source().sendMessage(Component.text(
                    "Unknown punishment reason: " + rawReason,
                    NamedTextColor.RED
            ));
            return;
        }

        long banRawTime = durations.get(rawReason);
        boolean isWiped = wipes.getOrDefault(rawReason, false);
        boolean isPermanent = Set.of("autopunish", "make-a-ticket").contains(rawReason);
        String banText = punishmentText.get(rawReason);

        if (banText == null) {
            invocation.source().sendMessage(Component.text(
                    "Missing punishment text for: " + rawReason,
                    NamedTextColor.RED
            ));
            return;
        }

        int targetOffendCount = (int) targetObject.getPunishments().stream()
                .filter(punishment -> banText.equals(punishment.getReason()))
                .count() + 1;

        long banTime = banRawTime * targetOffendCount;

        executeOffend(
                invocation,
                banTime,
                banText,
                targetOffendCount,
                targetObject,
                staffName,
                isWiped,
                staffNote,
                isPermanent
        );
    }

    private void executeOffend(
            Invocation invocation,
            long banTime,
            String banText,
            int offendCount,
            ProxyPlayerObject targetObject,
            String staffName,
            boolean isWiped,
            String staffNote,
            boolean isPermanent
    ) {
        String durationText;

        if (banTime < TimeUnit.DAYS.toMillis(1)) {
            long hours = TimeUnit.MILLISECONDS.toHours(banTime);
            durationText = hours + (hours == 1 ? " hour" : " hours");
        } else {
            long days = TimeUnit.MILLISECONDS.toDays(banTime);
            durationText = days + (days == 1 ? " day" : " days");
        }

        String ipAddress = proxy.getPlayer(targetObject.getUuid())
                .map(player -> player.getRemoteAddress().getAddress().getHostAddress())
                .orElse(null);

        ZonedDateTime createdAt = ZonedDateTime.now(ZoneId.of("Europe/Zurich"));
        ZonedDateTime expiresAt = isPermanent ? null : createdAt.plusSeconds(banTime / 1000);

        PlayerPunishment punishment = new PlayerPunishment(
                offendCount,
                banText,
                ipAddress,
                staffName,
                createdAt,
                expiresAt,
                isWiped,
                staffNote,
                BanIdGenerator.generateBanId(),
                isPermanent,
                true
        );

        targetObject.getPunishments().add(punishment);
        Goliath.playerRepository.save(targetObject);

        invocation.source().sendMessage(Component.text(
                "Punishment executed with success. Offension: " + offendCount,
                NamedTextColor.RED
        ));

        if (isPermanent) {
            invocation.source().sendMessage(
                    Component.text("Permanently banned player ", NamedTextColor.RED)
                            .append(Component.text(targetObject.getName(), NamedTextColor.WHITE))
                            .append(Component.text(" with reason:", NamedTextColor.RED))
            );
        } else {
            invocation.source().sendMessage(
                    Component.text("Temporarily banned player ", NamedTextColor.RED)
                            .append(Component.text(targetObject.getName(), NamedTextColor.WHITE))
                            .append(Component.text(" for ", NamedTextColor.RED))
                            .append(Component.text(durationText, NamedTextColor.WHITE))
                            .append(Component.text(" with reason:", NamedTextColor.RED))
            );
        }

        Component chatBanText = banText.contains("&")
                ? LegacyComponentSerializer.legacyAmpersand().deserialize(banText)
                : Component.text(banText, NamedTextColor.WHITE);

        invocation.source().sendMessage(chatBanText);

        Optional<Player> targetPlayer = proxy.getPlayer(targetObject.getUuid());

        if (targetPlayer.isPresent()) {
            Player target = targetPlayer.get();

            Component banMessage = banText.contains("&")
                    ? LegacyComponentSerializer.legacyAmpersand().deserialize(banText)
                    : Component.text(banText, NamedTextColor.RED);

            if (isPermanent) {
                target.disconnect(
                        banMessage
                                .appendNewline()
                                .appendNewline()
                                .append(Component.text("Date: ", NamedTextColor.GRAY))
                                .append(Component.text(createdAt.format(DATE_FORMATTER), NamedTextColor.WHITE))
                                .appendNewline()
                                .appendNewline()
                                .append(Component.text("Ban ID: ", NamedTextColor.GRAY))
                                .append(Component.text(punishment.getBanId(), NamedTextColor.WHITE))
                                .appendNewline()
                                .appendNewline()
                                .append(Component.text("You may be able to appeal this ban on", NamedTextColor.GRAY))
                                .appendNewline()
                                .append(Component.text("discord.gg/mcserver", NamedTextColor.WHITE))
                );
                return;
            }

            Duration remaining = Duration.between(
                    ZonedDateTime.now(ZoneId.of("Europe/Zurich")),
                    expiresAt
            );

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
                            .append(Component.text(punishment.getBanId(), NamedTextColor.WHITE))
                            .appendNewline()
                            .appendNewline()
                            .append(Component.text("You may be able to appeal this ban on", NamedTextColor.GRAY))
                            .appendNewline()
                            .append(Component.text("discord.gg/mcserver", NamedTextColor.WHITE))
            );
        }
    }

    private String checkOffendSource(Invocation invocation) {
        if (invocation.source() instanceof Player player) {
            return player.getUsername();
        }

        return "Console";
    }

    public static final HashMap<String, Long> durations = new HashMap<>() {{
        put("autopunish", 30L * 24 * 60 * 60 * 1000);

        put("ban-evading", 30L * 24 * 60 * 60 * 1000);
        put("bug-abuse", 14L * 24 * 60 * 60 * 1000);
        put("cross-trading", 30L * 24 * 60 * 60 * 1000);
        put("cheating", 14L * 24 * 60 * 60 * 1000);
        put("doxing", 365L * 24 * 60 * 60 * 1000);
        put("duping", 365L * 24 * 60 * 60 * 1000);

        put("external-gambling", 60L * 24 * 60 * 60 * 1000);
        put("flooding-chat", 1L * 24 * 60 * 60 * 1000);
        put("gambling-ownership", 90L * 24 * 60 * 60 * 1000);

        put("hacking", 60L * 24 * 60 * 60 * 1000);
        put("make-a-ticket", 30L * 24 * 60 * 60 * 1000);

        put("mute-evasion", 14L * 24 * 60 * 60 * 1000);
        put("macro-scripts", 7L * 24 * 60 * 60 * 1000);
        put("macros", 7L * 24 * 60 * 60 * 1000);

        put("invite-rewards", 14L * 24 * 60 * 60 * 1000);
        put("invite-rewards-ownership", 30L * 24 * 60 * 60 * 1000);

        put("radar/minimap", 30L * 24 * 60 * 60 * 1000);
        put("ratting", 365L * 24 * 60 * 60 * 1000);

        put("soundboard-proximity", 7L * 24 * 60 * 60 * 1000);
        put("spamming-chat", 1L * 24 * 60 * 60 * 1000);

        put("streaming-advertising-rat-clients", 365L * 24 * 60 * 60 * 1000);
        put("streaming-cheat-client-pvp", 30L * 24 * 60 * 60 * 1000);
        put("streaming-just-hack-client-no-advantage", 14L * 24 * 60 * 60 * 1000);

        put("suicide-encouragement-proximity", 30L * 24 * 60 * 60 * 1000);
        put("toxicity/hate/harassment-in-chat-sign", 7L * 24 * 60 * 60 * 1000);
        put("xray/esp/baritone", 60L * 24 * 60 * 60 * 1000);

        put("racism-in-chat-sign", 30L * 24 * 60 * 60 * 1000);
        put("proximity-racism", 30L * 24 * 60 * 60 * 1000);
        put("proximity-toxicity/hate/harassment", 7L * 24 * 60 * 60 * 1000);

        put("lying-to-staff", 14L * 24 * 60 * 60 * 1000);

        put("irl-trade/boosting", 60L * 24 * 60 * 60 * 1000);
        put("irl-trading-ownership", 365L * 24 * 60 * 60 * 1000);

        put("inappropriate-skin", 12L * 60 * 60 * 1000);
        put("inappropriate-name", 1L * 24 * 60 * 60 * 1000);
        put("inappropriate-map-art", 7L * 24 * 60 * 60 * 1000);
        put("inappropriate-proximity", 7L * 24 * 60 * 60 * 1000);
        put("inappropriate-language", 3L * 24 * 60 * 60 * 1000);
        put("inappropriate-builds", 14L * 24 * 60 * 60 * 1000);

        put("health-indicators", 14L * 24 * 60 * 60 * 1000);

        put("xray", 30L * 24 * 60 * 60 * 1000);
        put("esp", 60L * 24 * 60 * 60 * 1000);
        put("baritone", 30L * 24 * 60 * 60 * 1000);
    }};

    public static final HashMap<String, String> punishmentText = new HashMap<>() {{
        put("autopunish", "&f&l Your account has been placed on hold.\n &7 We need to chat with you about something \n \n &7 Please open a ticket in the Donut SMP Discord \n &7 abd we'll get you back as soon as possible!");
        put("ban-evading", "You are temporarily banned for joining on another account while being banned.");
        put("bug-abuse", "You are temporarily banned for abusing a bug/issue.");
        put("cross-trading", "You are temporarily banned for cross trading.");
        put("cheating", "You are temporarily banned for cheating.");
        put("doxing", "You are temporarily banned for doxing.");
        put("duping", "You are temporarily banned for duplicating items.");

        put("external-gambling", "You are temporarily banned for external gambling.");
        put("flooding-chat", "You are temporarily muted for flooding chat.");
        put("gambling-ownership", "You are temporarily banned for gambling ownership.");

        put("hacking", "You are temporarily banned for hacking.");
        put("make-a-ticket", "&f&l Your account has been placed on hold.\n &7 We need to chat with you about something \n \n &7 Please open a ticket in the Donut SMP Discord \n &7 abd we'll get you back as soon as possible!");

        put("mute-evasion", "You are temporarily banned for mute evasion.");
        put("macro-scripts", "You are temporarily banned for using macros or scripts.");
        put("macros", "You are temporarily banned for using macros or scripts.");

        put("invite-rewards", "You are temporarily banned for invite rewards abuse.");
        put("invite-rewards-ownership", "You are temporarily banned for invite rewards ownership.");

        put("radar/minimap", "You are temporarily banned for using radar or minimap modifications.");
        put("ratting", "You are temporarily banned for ratting.");

        put("soundboard-proximity", "You are temporarily muted for soundboard abuse in proximity chat.");
        put("spamming-chat", "You are temporarily muted for spamming chat.");

        put("streaming-advertising-rat-clients", "You are temporarily banned for advertising rat clients while streaming.");
        put("streaming-cheat-client-pvp", "You are temporarily banned for streaming cheat client gameplay.");
        put("streaming-just-hack-client-no-advantage", "You are temporarily banned for streaming a hack client.");

        put("suicide-encouragement-proximity", "You are temporarily banned for encouraging suicide.");
        put("toxicity/hate/harassment-in-chat-sign", "You are temporarily muted for toxicity, hate speech or harassment.");
        put("xray/esp/baritone", "You are temporarily banned for the use of X-Ray, ESP or baritone.");

        put("racism-in-chat-sign", "You are temporarily banned for racism.");
        put("proximity-racism", "You are temporarily banned for racial slurs in voice chat.");
        put("proximity-toxicity/hate/harassment", "You are temporarily muted for toxicity, hate speech or harassment.");

        put("lying-to-staff", "You are temporarily banned for lying to staff.");

        put("irl-trade/boosting", "You are temporarily banned for trading server items for real life goods or services.");
        put("irl-trading-ownership", "You are temporarily banned for trading server items for real life goods or services.");

        put("inappropriate-skin", "You are temporarily banned for having an inappropriate skin. Please change it before joining again.");
        put("inappropriate-name", "You are temporarily banned for having an inappropriate name. Please change it before joining again.");
        put("inappropriate-map-art", "You are temporarily banned for having inappropriate map art.");
        put("inappropriate-proximity", "You are temporarily muted for inappropriate proximity chat.");
        put("inappropriate-language", "You are temporarily muted for inappropriate language.");
        put("inappropriate-builds", "You are temporarily banned for creating inappropriate builds.");

        put("health-indicators", "You are temporarily banned for using health indicators.");

        put("xray", "You are temporarily banned for the use of X-Ray.");
        put("esp", "You are temporarily banned for the use of ESP.");
        put("baritone", "You are temporarily banned for the use of baritone.");
    }};

    public static final HashMap<String, Boolean> wipes = new HashMap<>() {{
        put("autopunish", false);

        put("ban-evading", true);
        put("bug-abuse", false);
        put("cross-trading", true);
        put("cheating", true);
        put("doxing", true);
        put("duping", true);

        put("external-gambling", true);
        put("flooding-chat", false);
        put("gambling-ownership", true);

        put("hacking", true);
        put("make-a-ticket", false);

        put("mute-evasion", false);
        put("macro-scripts", true);
        put("macros", true);

        put("invite-rewards", false);
        put("invite-rewards-ownership", true);

        put("radar/minimap", true);
        put("ratting", true);

        put("soundboard-proximity", false);
        put("spamming-chat", false);

        put("streaming-advertising-rat-clients", true);
        put("streaming-cheat-client-pvp", true);
        put("streaming-just-hack-client-no-advantage", false);

        put("suicide-encouragement-proximity", true);
        put("toxicity/hate/harassment-in-chat-sign", false);
        put("xray/esp/baritone", true);

        put("racism-in-chat-sign", true);
        put("proximity-racism", true);
        put("proximity-toxicity/hate/harassment", false);

        put("lying-to-staff", false);

        put("irl-trade/boosting", true);
        put("irl-trading-ownership", true);

        put("inappropriate-skin", false);
        put("inappropriate-name", false);
        put("inappropriate-map-art", false);
        put("inappropriate-proximity", false);
        put("inappropriate-language", false);
        put("inappropriate-builds", false);

        put("health-indicators", false);

        put("xray", true);
        put("esp", true);
        put("baritone", true);
    }};

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 0) {
            return proxy.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .toList();
        }

        if (args.length == 1) {
            String input = args[0].toLowerCase();

            return proxy.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .toList();
        }

        if (args.length == 2) {
            String input = args[1].toLowerCase();

            return durations.keySet().stream()
                    .filter(key -> key.toLowerCase().startsWith(input))
                    .sorted()
                    .toList();
        }

        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("goliath.staff.offend");
    }
}
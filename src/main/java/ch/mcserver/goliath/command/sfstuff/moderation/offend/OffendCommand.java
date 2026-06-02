package ch.mcserver.goliath.command.sfstuff.moderation.offend;

import ch.mcserver.goliath.Goliath;
import ch.mcserver.goliath.database.repository.mysql.PlayerRepository;
import ch.mcserver.goliath.player.ProxyPlayerObject;
import ch.mcserver.goliath.player.punishments.PlayerPunishment;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class OffendCommand implements SimpleCommand {

    private final ProxyServer proxy;

    public OffendCommand(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        String staffName = checkOffendSource(invocation);

        if (args.length < 2 || args.length > 3) {
            return;
        }

        String targetName = args[0];
        String staffNote = "null";

        if (args.length == 3) {
            staffNote = args[2];
        }

        PlayerRepository playerRepository = Goliath.playerRepository;

        if (!playerRepository.existsByUsername(targetName)) {
            invocation.source().sendMessage(Component.text("Player does not exist", NamedTextColor.RED));
            return;
        }

        ProxyPlayerObject targetObject = playerRepository.loadPlayerByUsername(targetName);

        if (targetObject.getPunishments() == null) {
            targetObject.setPunishments(new ArrayList<>());
        }

        int targetOffendCount = targetObject.getPunishments().size() + 1;
        String rawReason = args[1].toLowerCase();

        if (!durations.containsKey(rawReason)) {
            return;
        }

        long banRawTime = durations.get(rawReason);
        boolean isWiped = wipes.getOrDefault(rawReason, false);
        String banText = punishmentText.get(rawReason);

        if (banText == null) {
            invocation.source().sendMessage(Component.text("Missing punishment text for: " + rawReason, NamedTextColor.RED));
            return;
        }

        long banTime = banRawTime * targetOffendCount;

        executeOffend(
                invocation,
                banTime,
                banText,
                targetOffendCount,
                targetObject,
                staffName,
                isWiped,
                staffNote
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
            String staffNote
    ) {
        String durationText;

        if (banTime < TimeUnit.DAYS.toMillis(1)) {
            long hours = TimeUnit.MILLISECONDS.toHours(banTime);
            durationText = hours + " hour";
        } else {
            long days = TimeUnit.MILLISECONDS.toDays(banTime);
            durationText = days + " day";
        }

        ZonedDateTime createdAt = ZonedDateTime.now(ZoneId.of("Europe/Zurich"));
        ZonedDateTime expiresAt = createdAt.plusSeconds(banTime / 1000);

        PlayerPunishment playerPunishment = new PlayerPunishment(
                offendCount,
                banText,
                null,
                staffName,
                createdAt,
                expiresAt,
                isWiped,
                staffNote,
                false
        );

        targetObject.getPunishments().add(playerPunishment);

        PlayerRepository playerRepository = Goliath.playerRepository;
        playerRepository.save(targetObject);

        invocation.source().sendMessage(Component.text(
                "Punishment executed with success. Offension: " + offendCount,
                NamedTextColor.RED
        ));

        invocation.source().sendMessage(Component.text("Temporarily banned player ", NamedTextColor.RED)
                .append(Component.text(targetObject.getName(), NamedTextColor.WHITE))
                .append(Component.text(" for ", NamedTextColor.RED))
                .append(Component.text(durationText, NamedTextColor.WHITE))
                .append(Component.text(" with reason:", NamedTextColor.RED)));

        invocation.source().sendMessage(Component.text(banText, NamedTextColor.WHITE));

        Optional<Player> targetPlayer = proxy.getPlayer(targetObject.getUuid());

        if (targetPlayer.isPresent()) {
            Player target = targetPlayer.get();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            String dateText = playerPunishment.getCreatedAt().format(formatter);

            target.disconnect(Component.text("You are banned from this server!", NamedTextColor.RED)
                    .appendNewline()
                    .append(Component.text("Expires in: " + durationText, NamedTextColor.WHITE))
                    .appendNewline()
                    .append(Component.text("Date: " + dateText, NamedTextColor.WHITE))
                    .appendNewline()
                    .append(Component.text("Reason: " + banText, NamedTextColor.WHITE)));
        }
    }

    private String checkOffendSource(Invocation invocation) {
        if (invocation.source() instanceof Player player) {
            return player.getUsername();
        }

        return "Console";
    }

    public static HashMap<String, Long> durations = new HashMap<>() {{
        put("autopunish", 0L);

        put("ban-evading", 30L * 24 * 60 * 60 * 1000);
        put("bug-abuse", 14L * 24 * 60 * 60 * 1000);
        put("cross-trading", 30L * 24 * 60 * 60 * 1000);
        put("cheating", 30L * 24 * 60 * 60 * 1000);
        put("doxing", 365L * 24 * 60 * 60 * 1000);
        put("duping", 30L * 24 * 60 * 60 * 1000);

        put("external-gambling", 60L * 24 * 60 * 60 * 1000);
        put("flooding-chat", 1L * 24 * 60 * 60 * 1000);
        put("gambling-ownership", 90L * 24 * 60 * 60 * 1000);

        put("hacking", 60L * 24 * 60 * 60 * 1000);
        put("make-a-ticket", 0L);

        put("mute-evasion", 14L * 24 * 60 * 60 * 1000);
        put("macro-scripts", 30L * 24 * 60 * 60 * 1000);

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

        put("inappropriate-skin", 7L * 24 * 60 * 60 * 1000);
        put("inappropriate-proximity", 7L * 24 * 60 * 60 * 1000);
        put("inappropriate-language", 3L * 24 * 60 * 60 * 1000);
        put("inappropriate-builds", 14L * 24 * 60 * 60 * 1000);

        put("health-indicators", 14L * 24 * 60 * 60 * 1000);

        put("xray", 30L * 24 * 60 * 60 * 1000);
        put("esp", 60L * 24 * 60 * 60 * 1000);
        put("baritone", 30L * 24 * 60 * 60 * 1000);
    }};

    public static final HashMap<String, String> punishmentText = new HashMap<>() {{
        put("ban-evading", "You are temporarily banned for joining on another account while being banned.");
        put("bug-abuse", "You are temporarily banned for abusing a bug/issue");
        put("cross-trading", "You are temporarily banned for cross trading.");
        put("cheating", "You are temporarily banned for cheating.");
        put("doxing", "You are temporarily banned for doxing.");
        put("duping", "You are temporarily banned for duping.");

        put("external-gambling", "You are temporarily banned for external gambling.");
        put("flooding-chat", "You are temporarily muted for flooding chat.");
        put("gambling-ownership", "You are temporarily banned for gambling ownership.");

        put("hacking", "You are temporarily banned for hacking.");
        put("mute-evasion", "You are temporarily banned for mute evasion.");
        put("make-a-ticket", "You are temporarily banned for make a ticket.");

        put("macro-scripts", "You are temporarily banned for using macros or scripts.");

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
        put("proximity-racism", "You are temporarily banned for racism.");
        put("proximity-toxicity/hate/harassment", "You are temporarily muted for toxicity, hate speech or harassment.");

        put("lying-to-staff", "You are temporarily banned for lying to staff.");

        put("irl-trade/boosting", "You are temporarily banned for trading server items for real life goods or services.");
        put("irl-trading-ownership", "You are temporarily banned for IRL trading ownership.");

        put("inappropriate-skin", "You are temporarily banned for having an inappropriate skin.");
        put("inappropriate-proximity", "You are temporarily muted for inappropriate proximity chat.");
        put("inappropriate-language", "You are temporarily muted for inappropriate language.");
        put("inappropriate-builds", "You are temporarily banned for creating inappropriate builds.");

        put("health-indicators", "You are temporarily banned for using health indicators.");

        put("xray", "You are temporarily banned for the use of X-Ray.");
        put("esp", "You are temporarily banned for the use of ESP.");
        put("baritone", "You are temporarily banned for the use of baritone.");
    }};

    public static HashMap<String, Boolean> wipes = new HashMap<>() {{
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
            return proxy.getAllPlayers()
                    .stream()
                    .map(Player::getUsername)
                    .toList();
        }

        if (args.length == 1) {
            String input = args[0].toLowerCase();

            return proxy.getAllPlayers()
                    .stream()
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .toList();
        }

        if (args.length == 2) {
            String input = args[1].toLowerCase();

            return wipes.keySet()
                    .stream()
                    .filter(key -> key.toLowerCase().startsWith(input))
                    .toList();
        }

        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("GoliathCommand.offend");
    }
}
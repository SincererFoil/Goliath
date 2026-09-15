package ch.mcserver.goliath.anticheat.alert;

import ch.mcserver.goliath.anticheat.AnticheatFlagMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

public class AnticheatAlert {

    public static void sendAlert(AnticheatFlagMessage flagMessage) {
        TextComponent message = Component.text("[", NamedTextColor.GRAY)
                .append(Component.text("SUS", NamedTextColor.AQUA))
                .append(Component.text("] ", NamedTextColor.GRAY))
                .append(Component.text(flagMessage.playerName(), NamedTextColor.GRAY))
                .append(Component.text(" FAILED", NamedTextColor.GRAY));
    }
}

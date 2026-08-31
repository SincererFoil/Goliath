package ch.mcserver.goliath.player.alts;

import java.time.ZonedDateTime;
import java.util.UUID;

public record LinkedAccount(

        UUID uuid,
        String name,
        ZonedDateTime lastLinked

) {
}

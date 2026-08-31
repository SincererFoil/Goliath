package ch.mcserver.goliath.player.alts;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

public class IpHasher {

    private final SecretKeySpec key;

    public IpHasher(String secret) {
        this.key = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
    }

    public String hash(String ipAddress) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);

            byte[] result = mac.doFinal(
                    ipAddress.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(result);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not hash IP address", exception);
        }
    }
}
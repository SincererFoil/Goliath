package ch.mcserver.goliath.player.alts;

import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

public class GeoIpService {

    private final DatabaseReader reader;

    public GeoIpService(File databaseFile) throws IOException {
        this.reader = new DatabaseReader.Builder(databaseFile)
                .withCache(new CHMCache())
                .build();
    }

    public GeoLocation lookup(InetAddress address) {
        try {
            CityResponse response = reader.city(address);

            String city = response.city().name();
            String region = response.mostSpecificSubdivision().name();
            String country = response.country().name();
            Integer radius = response.location().accuracyRadius();

            return new GeoLocation(
                    city == null ? "Unknown" : city,
                    region == null ? "Unknown" : region,
                    country == null ? "Unknown" : country,
                    radius == null ? 0 : radius
            );
        } catch (Exception exception) {
            return new GeoLocation("Unknown", "Unknown", "Unknown", 0);
        }
    }

    public void close() throws IOException {
        reader.close();
    }
}
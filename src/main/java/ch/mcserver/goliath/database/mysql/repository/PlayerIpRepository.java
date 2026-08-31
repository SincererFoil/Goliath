package ch.mcserver.goliath.database.mysql.repository;

import ch.mcserver.goliath.database.mysql.MySQLManager;
import ch.mcserver.goliath.player.alts.GeoLocation;
import ch.mcserver.goliath.player.alts.LinkedAccount;

import java.sql.*;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerIpRepository {

    private final MySQLManager mySQLManager;

    public PlayerIpRepository(MySQLManager mySQLManager) {
        this.mySQLManager = mySQLManager;
    }

    public List<LinkedAccount> getLinkedAccounts(UUID playerUuid) {
        List<LinkedAccount> accounts = new ArrayList<>();
        Connection connection = mySQLManager.getConnection();

        try (PreparedStatement statement = connection.prepareStatement(
                """
                SELECT players.uuid, players.name,
                       MAX(LEAST(target_ip.last_seen, linked_ip.last_seen)) AS last_linked
                FROM player_ip_history target_ip
                INNER JOIN player_ip_history linked_ip ON linked_ip.ip_hash = target_ip.ip_hash
                INNER JOIN players ON players.uuid = linked_ip.player_uuid
                WHERE target_ip.player_uuid = ?
                  AND linked_ip.player_uuid != target_ip.player_uuid
                GROUP BY players.uuid, players.name
                ORDER BY last_linked DESC
                """
        )) {
            statement.setString(1, playerUuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Timestamp lastLinked = resultSet.getTimestamp("last_linked");

                    accounts.add(new LinkedAccount(
                            UUID.fromString(resultSet.getString("uuid")),
                            resultSet.getString("name"),
                            lastLinked.toLocalDateTime().atZone(ZoneId.of("Europe/Zurich"))
                    ));
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        return accounts;
    }

    public GeoLocation getLatestLocation(UUID playerUuid) {
        Connection connection = mySQLManager.getConnection();

        try (PreparedStatement statement = connection.prepareStatement(
                """
                SELECT ip_locations.city_name,
                       ip_locations.region_name,
                       ip_locations.country_name,
                       ip_locations.accuracy_radius
                FROM player_ip_history
                INNER JOIN ip_locations
                    ON ip_locations.ip_hash = player_ip_history.ip_hash
                WHERE player_ip_history.player_uuid = ?
                ORDER BY player_ip_history.last_seen DESC
                LIMIT 1
                """
        )) {
            statement.setString(1, playerUuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                return new GeoLocation(
                        resultSet.getString("city_name"),
                        resultSet.getString("region_name"),
                        resultSet.getString("country_name"),
                        resultSet.getInt("accuracy_radius")
                );
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public void save(UUID playerUuid, String ipHash, GeoLocation location) {
        Connection connection = mySQLManager.getConnection();

        try (PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO ip_locations(
                    ip_hash, city_name, region_name, country_name,
                    accuracy_radius, last_updated
                ) VALUES (?, ?, ?, ?, ?, NOW())
                ON DUPLICATE KEY UPDATE
                    city_name = VALUES(city_name),
                    region_name = VALUES(region_name),
                    country_name = VALUES(country_name),
                    accuracy_radius = VALUES(accuracy_radius),
                    last_updated = NOW()
                """
        )) {
            statement.setString(1, ipHash);
            statement.setString(2, location.city());
            statement.setString(3, location.region());
            statement.setString(4, location.country());
            statement.setInt(5, location.accuracyRadius());
            statement.executeUpdate();
        } catch (SQLException exception) {
            exception.printStackTrace();
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO player_ip_history(
                    player_uuid, ip_hash, first_seen,
                    last_seen, connection_count
                ) VALUES (?, ?, NOW(), NOW(), 1)
                ON DUPLICATE KEY UPDATE
                    last_seen = NOW(),
                    connection_count = connection_count + 1
                """
        )) {
            statement.setString(1, playerUuid.toString());
            statement.setString(2, ipHash);
            statement.executeUpdate();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }
}
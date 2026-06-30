package ch.mcserver.goliath.database.mysql.repository;

import ch.mcserver.goliath.database.mysql.MySQLManager;
import ch.mcserver.goliath.player.ProxyPlayerObject;
import ch.mcserver.goliath.player.punishments.PlayerPunishment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerRepository {

    private final MySQLManager mySQLManager;
    private static final ZoneId ZONE = ZoneId.of("Europe/Zurich");

    public PlayerRepository(MySQLManager mySQLManager) {
        this.mySQLManager = mySQLManager;
    }

    public boolean exists(UUID uuid) {
        Connection connection = mySQLManager.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM players WHERE uuid = ?"
        )) {

            statement.setString(1, uuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }

        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        return false;
    }

    public boolean existsByUsername(String name) {
        Connection connection = mySQLManager.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM players WHERE name = ?"
        )) {

            statement.setString(1, name);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }

        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        return false;
    }

    public ProxyPlayerObject loadPlayer(UUID uuid) {
        Connection connection = mySQLManager.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM players WHERE uuid = ?"
        )) {

            statement.setString(1, uuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {

                if (!resultSet.next()) {
                    return null;
                }

                UUID playerUuid = UUID.fromString(resultSet.getString("uuid"));

                return new ProxyPlayerObject(
                        playerUuid,
                        resultSet.getString("name"),
                        resultSet.getString("prefix"),
                        resultSet.getString("current_server"),
                        resultSet.getBoolean("sfmode"),
                        resultSet.getBoolean("debug_mode"),
                        resultSet.getBoolean("gmsp"),
                        resultSet.getFloat("fly_speed"),
                        resultSet.getLong("first_join"),
                        resultSet.getLong("last_join"),
                        resultSet.getBoolean("creative"),
                        loadPunishments(playerUuid)
                );
            }

        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        return null;
    }

    public ProxyPlayerObject loadPlayerByUsername(String username) {
        Connection connection = mySQLManager.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM players WHERE name = ?"
        )) {

            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (!resultSet.next()) {
                    return null;
                }

                UUID playerUuid = UUID.fromString(resultSet.getString("uuid"));

                return new ProxyPlayerObject(
                        playerUuid,
                        resultSet.getString("name"),
                        resultSet.getString("prefix"),
                        resultSet.getString("current_server"),
                        resultSet.getBoolean("sfmode"),
                        resultSet.getBoolean("debug_mode"),
                        resultSet.getBoolean("gmsp"),
                        resultSet.getFloat("fly_speed"),
                        resultSet.getLong("first_join"),
                        resultSet.getLong("last_join"),
                        resultSet.getBoolean("creative"),
                        loadPunishments(playerUuid)
                );
            }

        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        return null;
    }

    private ArrayList<PlayerPunishment> loadPunishments(UUID uuid) {

        ArrayList<PlayerPunishment> punishments = new ArrayList<>();

        Connection connection = mySQLManager.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM player_punishments WHERE player_uuid = ?"
        )) {

            statement.setString(1, uuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {

                    Timestamp createdAtTimestamp = resultSet.getTimestamp("created_at");
                    Timestamp expiresAtTimestamp = resultSet.getTimestamp("expires_at");

                    punishments.add(
                            new PlayerPunishment(
                                    resultSet.getInt("offense_level"),
                                    resultSet.getString("reason"),
                                    null,
                                    resultSet.getString("punished_by"),
                                    createdAtTimestamp.toLocalDateTime().atZone(ZONE),
                                    expiresAtTimestamp == null
                                            ? null
                                            : expiresAtTimestamp.toLocalDateTime().atZone(ZONE),
                                    resultSet.getBoolean("wiped"),
                                    resultSet.getString("staff_note"),
                                    resultSet.getBoolean("permanent")
                            )
                    );
                }
            }

        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        return punishments;
    }

    public void create(ProxyPlayerObject proxyPlayerObject) {
        Connection connection = mySQLManager.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO players(
                    uuid, name, prefix, shards, money, kills, deaths,
                    blocks_broken, blocks_placed, sfmode, debug_mode,
                    gmsp, fly_speed, current_server, first_join, last_join, creative
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """
        )) {

            statement.setString(1, proxyPlayerObject.getUuid().toString());
            statement.setString(2, proxyPlayerObject.getName());
            statement.setString(3, proxyPlayerObject.getPrefix());

            statement.setInt(4, 0);
            statement.setDouble(5, 0.0);
            statement.setInt(6, 0);
            statement.setInt(7, 0);
            statement.setInt(8, 0);
            statement.setInt(9, 0);

            statement.setBoolean(10, proxyPlayerObject.isSfmode());
            statement.setBoolean(11, proxyPlayerObject.isDebugMode());
            statement.setBoolean(12, proxyPlayerObject.isGmsp());

            statement.setFloat(13, proxyPlayerObject.getFlySpeed());
            statement.setString(14, proxyPlayerObject.getCurrentServer());
            statement.setLong(15, proxyPlayerObject.getFirstJoin());
            statement.setLong(16, proxyPlayerObject.getLastJoin());
            statement.setBoolean(17, proxyPlayerObject.isCreative());

            statement.executeUpdate();

        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public void save(ProxyPlayerObject proxyPlayerObject) {
        Connection connection = mySQLManager.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(
                """
                UPDATE players
                SET name = ?, prefix = ?, sfmode = ?, debug_mode = ?, gmsp = ?,
                    fly_speed = ?, current_server = ?, last_join = ?, creative = ?
                WHERE uuid = ?
                """
        )) {

            statement.setString(1, proxyPlayerObject.getName());
            statement.setString(2, proxyPlayerObject.getPrefix());
            statement.setBoolean(3, proxyPlayerObject.isSfmode());
            statement.setBoolean(4, proxyPlayerObject.isDebugMode());
            statement.setBoolean(5, proxyPlayerObject.isGmsp());
            statement.setFloat(6, proxyPlayerObject.getFlySpeed());
            statement.setString(7, proxyPlayerObject.getCurrentServer());
            statement.setLong(8, proxyPlayerObject.getLastJoin());
            statement.setBoolean(9, proxyPlayerObject.isCreative());
            statement.setString(10, proxyPlayerObject.getUuid().toString());

            statement.executeUpdate();

            try (PreparedStatement deletePunishments = connection.prepareStatement(
                    "DELETE FROM player_punishments WHERE player_uuid = ?"
            )) {
                deletePunishments.setString(1, proxyPlayerObject.getUuid().toString());
                deletePunishments.executeUpdate();
            }

            for (PlayerPunishment punishment : proxyPlayerObject.getPunishments()) {
                savePunishment(proxyPlayerObject.getUuid(), punishment);
            }

        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public void savePunishment(UUID playerUuid, PlayerPunishment punishment) {
        Connection connection = mySQLManager.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO player_punishments(
                    player_uuid, offense_level, reason, punished_by,
                    created_at, expires_at, wiped, staff_note, permanent
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """
        )) {

            statement.setString(1, playerUuid.toString());
            statement.setInt(2, punishment.getOffenseLevel());
            statement.setString(3, punishment.getReason());
            statement.setString(4, punishment.getPunishedBy());

            statement.setTimestamp(
                    5,
                    Timestamp.valueOf(punishment.getCreatedAt().toLocalDateTime())
            );

            if (punishment.getExpiresAt() == null) {
                statement.setTimestamp(6, null);
            } else {
                statement.setTimestamp(
                        6,
                        Timestamp.valueOf(punishment.getExpiresAt().toLocalDateTime())
                );
            }

            statement.setBoolean(7, punishment.isWiped());
            statement.setString(8, punishment.getStaffNote());
            statement.setBoolean(9, punishment.isPermanent());

            statement.executeUpdate();

        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public void savePlayerDataOnly(ProxyPlayerObject proxyPlayerObject) {
        Connection connection = mySQLManager.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(
                """
                UPDATE players
                SET name = ?, prefix = ?, sfmode = ?, debug_mode = ?, gmsp = ?,
                    fly_speed = ?, current_server = ?, last_join = ?, creative = ?
                WHERE uuid = ?
                """
        )) {

            statement.setString(1, proxyPlayerObject.getName());
            statement.setString(2, proxyPlayerObject.getPrefix());
            statement.setBoolean(3, proxyPlayerObject.isSfmode());
            statement.setBoolean(4, proxyPlayerObject.isDebugMode());
            statement.setBoolean(5, proxyPlayerObject.isGmsp());
            statement.setFloat(6, proxyPlayerObject.getFlySpeed());
            statement.setString(7, proxyPlayerObject.getCurrentServer());
            statement.setLong(8, proxyPlayerObject.getLastJoin());
            statement.setBoolean(9, proxyPlayerObject.isCreative());
            statement.setString(10, proxyPlayerObject.getUuid().toString());

            statement.executeUpdate();

        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public List<String> getAllUsernames() {
        List<String> usernames = new ArrayList<>();
        Connection connection = mySQLManager.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT name FROM players"
        );
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                usernames.add(resultSet.getString("name"));
            }

        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return usernames;
    }

    public List<String> getAllPunishedUsernames() {
        List<String> usernames = new ArrayList<>();
        Connection connection = mySQLManager.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT DISTINCT players.name FROM players " +
                        "INNER JOIN player_punishments ON players.uuid = player_punishments.player_uuid"
        );
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                usernames.add(resultSet.getString("name"));
            }

        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return usernames;
    }
}
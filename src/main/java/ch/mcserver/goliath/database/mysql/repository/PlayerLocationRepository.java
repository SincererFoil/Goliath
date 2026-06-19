package ch.mcserver.goliath.database.mysql.repository;

import ch.mcserver.goliath.database.mysql.MySQLManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

public class PlayerLocationRepository {

    private final MySQLManager mySQLManager;

    public PlayerLocationRepository(MySQLManager mySQLManager) {
        this.mySQLManager = mySQLManager;
    }

    public PlayerLocationObject loadPlayer(UUID uuid) {
        String sql = """
                SELECT uuid, created_at, yaw, pitch, x, y, z, server
                FROM player_location
                WHERE uuid = ?
                """;

        Connection connection = mySQLManager.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, uuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                return new PlayerLocationObject(
                        UUID.fromString(resultSet.getString("uuid")),
                        resultSet.getTimestamp("created_at"),
                        resultSet.getFloat("yaw"),
                        resultSet.getFloat("pitch"),
                        resultSet.getInt("x"),
                        resultSet.getInt("y"),
                        resultSet.getInt("z"),
                        resultSet.getString("server")
                );
            }

        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public void save(PlayerLocationObject object) {
        String sql = """
                INSERT INTO player_location
                (uuid, created_at, yaw, pitch, x, y, z, server)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    created_at = VALUES(created_at),
                    yaw = VALUES(yaw),
                    pitch = VALUES(pitch),
                    x = VALUES(x),
                    y = VALUES(y),
                    z = VALUES(z),
                    server = VALUES(server)
                """;

        Connection connection = mySQLManager.getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, object.getUuid().toString());
            statement.setTimestamp(2, object.getUpdatedAt());
            statement.setFloat(3, object.getYaw());
            statement.setFloat(4, object.getPitch());
            statement.setInt(5, object.getX());
            statement.setInt(6, object.getY());
            statement.setInt(7, object.getZ());
            statement.setString(8, object.getServerName());

            statement.executeUpdate();

        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
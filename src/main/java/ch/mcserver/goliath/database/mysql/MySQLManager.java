package ch.mcserver.goliath.database.mysql;

import ch.mcserver.goliath.Goliath;

import java.sql.Connection;
import java.sql.DriverManager;

public class MySQLManager {

    private Connection connection;

    public void connect() {

        try {

            String host = Goliath.config.node("mysql", "host").getString("127.0.0.1");
            int port = Goliath.config.node("mysql", "port").getInt(3306);
            String database = Goliath.config.node("mysql", "database").getString("goliath");
            String username = Goliath.config.node("mysql", "username").getString("goliath");
            String password = Goliath.config.node("mysql", "password").getString("");

            Class.forName("com.mysql.cj.jdbc.Driver");

            connection = DriverManager.getConnection(
                    "jdbc:mysql://" + host + ":" + port + "/" + database +
                            "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                    username,
                    password
            );

            Goliath.LOGGER.info("[Goliath] MySQL connected.");

        } catch (Exception exception) {

            Goliath.LOGGER.warn("[Goliath] MySQL connection failed.");
            exception.printStackTrace();
        }
    }

    public Connection getConnection() {

        try {

            if (connection == null || connection.isClosed()) {
                connect();
            }

        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return connection;
    }

    public void disconnect() {

        try {

            if (connection != null && !connection.isClosed()) {
                connection.close();
                Goliath.LOGGER.info("[Goliath] MySQL connection closed!");
            }

        } catch (Exception exception) {
            exception.printStackTrace();
            Goliath.LOGGER.error("[Goliath]  Crashed! Reason: " + exception.getMessage());
        }
    }
}

package com.agritech.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Central JDBC connection manager for the local MySQL database.
 * Secrets are read from environment variables, never from source code.
 */
public final class DatabaseManager {
    private final String url;
    private final String user;
    private final String password;

    public DatabaseManager() {
        String explicitUrl = env("DB_URL");
        String host = envOrDefault("DB_HOST", "localhost");
        String port = envOrDefault("DB_PORT", "3306");
        String database = envOrDefault("DB_NAME", "agritech_intelligence");

        this.url = explicitUrl != null
                ? explicitUrl
                : "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata"
                + "&characterEncoding=UTF-8&useUnicode=true";
        this.user = envOrDefault("DB_USER", "root");
        this.password = System.getenv("DB_PASSWORD") == null ? "" : System.getenv("DB_PASSWORD");
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public void verifyReady() {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1");
             ResultSet result = statement.executeQuery()) {
            if (!result.next()) {
                throw new IllegalStateException("MySQL connection verify nahi ho paya.");
            }
        } catch (SQLException e) {
            throw databaseError(e);
        }

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT farmer_id FROM farmers LIMIT 1")) {
            statement.executeQuery();
        } catch (SQLException e) {
            if ("42S02".equals(e.getSQLState()) || e.getErrorCode() == 1146) {
                throw new IllegalStateException(
                        "MySQL connected hai, lekin AgriTech tables nahi mili. "
                                + "MySQL Workbench me sql/01_agritech_schema.sql run kijiye.", e);
            }
            throw databaseError(e);
        }
    }

    public boolean isReachable() {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1");
             ResultSet result = statement.executeQuery()) {
            return result.next();
        } catch (SQLException e) {
            return false;
        }
    }

    public String displayTarget() {
        String host = envOrDefault("DB_HOST", "localhost");
        String port = envOrDefault("DB_PORT", "3306");
        String database = envOrDefault("DB_NAME", "agritech_intelligence");
        return host + ":" + port + "/" + database;
    }

    private IllegalStateException databaseError(SQLException e) {
        String message = "MySQL/JDBC connection failed.";
        if (e.getErrorCode() == 1045) {
            message += " Username/password check kijiye aur .\\setup.ps1 dobara run kijiye.";
        } else if (e.getErrorCode() == 1049) {
            message += " Database create nahi hua. MySQL Workbench me sql/01_agritech_schema.sql run kijiye.";
        } else if (e.getMessage() != null && e.getMessage().toLowerCase().contains("communications link failure")) {
            message += " MySQL Server start hai ya nahi check kijiye.";
        }
        return new IllegalStateException(message + " MySQL says: " + safe(e.getMessage()), e);
    }

    private static String env(String name) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String envOrDefault(String name, String fallback) {
        String value = env(name);
        return value == null ? fallback : value;
    }

    private static String safe(String value) {
        if (value == null) return "Unknown database error.";
        return value.replaceAll("(?i)password=[^&\\s]+", "password=***");
    }
}

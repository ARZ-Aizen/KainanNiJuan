package com.kainanresto.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConfig {

    private static HikariDataSource dataSource;

    static {
        Properties properties = new Properties();

        try (InputStream input = DatabaseConfig.class.getResourceAsStream("/db.properties")) {
            if (input == null) {
                System.err.println("DatabaseConfig: db.properties not found! Falling back to defaults.");
            } else {
                properties.load(input);
            }
        } catch (IOException e) {
            System.err.println("DatabaseConfig: Error reading db.properties: " + e.getMessage());
        }

        String host = properties.getProperty("db.host", "localhost");
        String port = properties.getProperty("db.port", "3306");
        String dbName = properties.getProperty("db.name", "kainan_pos");
        String user = properties.getProperty("db.user", "root");
        String password = properties.getProperty("db.password", "");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mariadb://" + host + ":" + port + "/" + dbName);
        config.setUsername(user);
        config.setPassword(password);
        config.setDriverClassName("org.mariadb.jdbc.Driver");

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(10000);
        config.setMaxLifetime(1800000);

        try {
            dataSource = new HikariDataSource(config);
        } catch (Exception e) {
            System.err.println("DatabaseConfig: Failed to initialize connection pool: " + e.getMessage());
        }
    }

    private DatabaseConfig() {
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource not initialized. Check XAMPP MariaDB and db.properties.");
        }
        return dataSource.getConnection();
    }

    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    /*

    public static void main(String[] args) {
        System.out.println("Testing MariaDB connection via HikariCP...");
        try (Connection conn = getConnection()) {
            System.out.println("Connection status: SUCCESS!");
            System.out.println("Connected to: " + conn.getMetaData().getDatabaseProductName() + " " + conn.getMetaData().getDatabaseProductVersion());
        } catch (SQLException e) {
            System.err.println ("Connection status: FAILED!");
            e.printStackTrace();
        } finally {
            closePool();
        }
    }


     */

}
package com.kainanresto.config;

// HIKARI
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

// SQL & IO
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public final class DatabaseConfig {

    private static HikariDataSource dataSource = null;
    private static Throwable initError = null;
    private static Properties loadedProperties = new Properties();
    private static String serverHost = "localhost";

    static {
        initPool();
    }

    private static synchronized void initPool() {
        if (dataSource != null && !dataSource.isClosed())
            return;

        Properties properties = new Properties();

        // 1. Load bundled defaults from inside the JAR
        try (InputStream input = DatabaseConfig.class.getResourceAsStream("/db.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException ignored) {
        }

        // 2. Candidate external locations for db.properties:
        // a) Current working directory: "db.properties"
        // b) Next to the executable/JAR install folder: "{app}/db.properties"
        // c) User AppData: "%APPDATA%/KainanNiJuan/db.properties"
        File externalFile = new File("db.properties");
        if (!externalFile.exists()) {
            try {
                File codeLocation = new File(
                        DatabaseConfig.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                File jarDir = codeLocation.getParentFile();
                if (jarDir != null) {
                    File rootDir = jarDir.getParentFile();
                    if (rootDir != null && new File(rootDir, "db.properties").exists()) {
                        externalFile = new File(rootDir, "db.properties");
                    } else if (new File(jarDir, "db.properties").exists()) {
                        externalFile = new File(jarDir, "db.properties");
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (!externalFile.exists()) {
            String appData = System.getenv("APPDATA");
            if (appData != null) {
                File appDataFile = new File(
                        appData + File.separator + "KainanNiJuan" + File.separator + "db.properties");
                if (appDataFile.exists())
                    externalFile = appDataFile;
            }
        }

        if (!externalFile.exists()) {
            File distFile = new File("dist" + File.separator + "KainanNiJuan" + File.separator + "db.properties");
            if (distFile.exists()) {
                externalFile = distFile;
            }
        }

        if (externalFile.exists()) {
            try (FileInputStream fis = new FileInputStream(externalFile)) {
                properties.load(fis);
                System.out.println(
                        "DatabaseConfig: Loaded external configuration from " + externalFile.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("DatabaseConfig: Error reading external db.properties: " + e.getMessage());
            }
        }

        String sysHost = System.getProperty("kainan.server", System.getProperty("db.host"));
        if (sysHost != null && !sysHost.isBlank()) {
            properties.setProperty("db.host", sysHost.trim());
            properties.setProperty("server.host", sysHost.trim());
            properties.setProperty("image.host", sysHost.trim());
        }

        loadedProperties = properties;
        serverHost = properties.getProperty("image.host",
                properties.getProperty("server.host",
                properties.getProperty("db.host", "192.168.141.130"))).trim();

        String host = properties.getProperty("db.host", "192.168.141.130");
        String port = properties.getProperty("db.port", "3306");
        String dbName = properties.getProperty("db.name", "kainan_pos");
        String user = properties.getProperty("db.user", "userpos");
        String password = properties.getProperty("db.password", "pos123");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mariadb://" + host + ":" + port + "/" + dbName);
        config.setUsername(user);
        config.setPassword(password);
        config.setDriverClassName("org.mariadb.jdbc.Driver");

        // CONNECTION POOL SIZING AND TIMEOUTS
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(10000);
        config.setMaxLifetime(1800000);

        try {
            dataSource = new HikariDataSource(config);
            initError = null;
        } catch (Exception e) {
            initError = e;
            System.err.println("DatabaseConfig: Pool initialization failed: " + e.getMessage());
        }
    }

    public static String getServerHost() {
        if (dataSource == null || dataSource.isClosed()) {
            initPool();
        }
        return serverHost;
    }

    public static Properties getProperties() {
        if (dataSource == null || dataSource.isClosed()) {
            initPool();
        }
        return loadedProperties;
    }

    private DatabaseConfig() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            initPool();
        }
        if (dataSource == null || dataSource.isClosed()) {
            String msg = initError != null ? initError.getMessage() : "Database connection pool is not available.";
            throw new SQLException("Could not connect to database: " + msg, initError);
        }
        return dataSource.getConnection();
    }

    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            dataSource = null;
        }
    }

    // PANG TEST NG CONNECTION
    public static void main(String[] args) {
        System.out.println("Testing MariaDB connection via HikariCP...");
        try (Connection conn = getConnection()) {
            System.out.println("Connection status: SUCCESS!");
            System.out.println("Connected to: " + conn.getMetaData().getDatabaseProductName() + " "
                    + conn.getMetaData().getDatabaseProductVersion());
        } catch (SQLException e) {
            System.err.println("Connection status: FAILED!");
            e.printStackTrace();
        } finally {
            closePool();
        }
    }
}
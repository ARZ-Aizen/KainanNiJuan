package com.kainanresto.config;

import com.kainanresto.config.DatabaseConfig;
import com.kainanresto.util.PasswordHasher; // Imported your hashing utility

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseSeeder {

    public static void seedAdmin() {
        // The query omits user_id, last_login, and created_at to use default DB values.
        String sql = "INSERT INTO users (username, password, role, full_name, is_active) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "admin");

            // Hash the plain text password before inserting
            String hashedPassword = PasswordHasher.hash("admin123");
            pstmt.setString(2, hashedPassword);

            pstmt.setString(3, "ADMIN");
            pstmt.setString(4, "System Administrator");
            pstmt.setInt(5, 1);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Admin user seeded successfully with a hashed password.");
            }

        } catch (SQLException e) {
            // SQLState 23000 catches unique constraint violations (e.g., username already exists)
            if ("23000".equals(e.getSQLState())) {
                System.out.println("Admin user already exists. Seeding skipped.");
            } else {
                System.err.println("Error seeding database: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("Starting database seeder...");
        seedAdmin();

        // Properly close the HikariCP pool
        DatabaseConfig.closePool();
        System.out.println("Seeding process completed.");
    }
}
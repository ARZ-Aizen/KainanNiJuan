package com.kainanresto.dao;

import com.kainanresto.config.DatabaseConfig;
import com.kainanresto.model.Role;
import com.kainanresto.model.User;
import com.kainanresto.util.PasswordHasher;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO {

    private static final int MYSQL_DUPLICATE_KEY_ERROR = 1062;

    public enum OperationResult {
        SUCCESS,
        DUPLICATE_ENTRY,
        NOT_FOUND,
        DATABASE_ERROR
    }

    // LOGIN
    public Optional<User> login(String username, String plainPassword) {
        String sql = "SELECT user_id, username, password, role, full_name, is_active " +
                "FROM users WHERE username = ? AND is_active = 1 LIMIT 1";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password");
                    if (PasswordHasher.verify(plainPassword, storedHash)) {
                        return Optional.of(mapResultSetToUser(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("UserDAO.login: DB Error - " + e.getMessage());
        }
        return Optional.empty();
    }

    // REGISTER
    public OperationResult register(User user) {
        String sql = "INSERT INTO users (username, password, role, full_name, is_active) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, PasswordHasher.hash(user.getPassword()));
            stmt.setString(3, user.getRole().name());
            stmt.setString(4, user.getFullName());
            stmt.setBoolean(5, user.isActive());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        user.setUserId(keys.getInt(1));
                    }
                }
                return OperationResult.SUCCESS;
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            if (e.getErrorCode() == MYSQL_DUPLICATE_KEY_ERROR) {
                return OperationResult.DUPLICATE_ENTRY;
            }
        } catch (SQLException e) {
            System.err.println("UserDAO.register: DB Error - " + e.getMessage());
        }
        return OperationResult.DATABASE_ERROR;
    }

    // RESET PASSWORD
    public OperationResult resetPassword(String username, String newPlainPassword) {
        String sql = "UPDATE users SET password = ? WHERE username = ? AND is_active = 1";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, PasswordHasher.hash(newPlainPassword));
            stmt.setString(2, username);

            int rows = stmt.executeUpdate();
            return (rows > 0) ? OperationResult.SUCCESS : OperationResult.NOT_FOUND;

        } catch (SQLException e) {
            System.err.println("UserDAO.resetPassword: DB Error - " + e.getMessage());
            return OperationResult.DATABASE_ERROR;
        }
    }

    // SOFT-DELETE USER
    public OperationResult deleteUser(int userId) {
        String sql = "UPDATE users SET is_active = 0 WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            int rows = stmt.executeUpdate();
            return (rows > 0) ? OperationResult.SUCCESS : OperationResult.NOT_FOUND;

        } catch (SQLException e) {
            System.err.println("UserDAO.deleteUser: DB Error - " + e.getMessage());
            return OperationResult.DATABASE_ERROR;
        }
    }

    // RETRIEVE ALL ACTIVE USERS
    public List<User> findAllActive() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT user_id, username, password, role, full_name, is_active " +
                "FROM users WHERE is_active = 1 ORDER BY full_name ASC";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("UserDAO.findAllActive: DB Error - " + e.getMessage());
        }
        return users;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("user_id"),
                rs.getString("username"),
                rs.getString("password"),
                Role.fromString(rs.getString("role")),
                rs.getString("full_name"),
                rs.getBoolean("is_active")
        );
    }
}
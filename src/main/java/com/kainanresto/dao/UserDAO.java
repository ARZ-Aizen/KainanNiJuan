package com.kainanresto.dao;

import com.kainanresto.config.DatabaseConfig;
import com.kainanresto.model.account.AccountRole;
import com.kainanresto.model.account.AccountRow;
import com.kainanresto.model.account.AccountStatus;
import com.kainanresto.model.account.User;
import com.kainanresto.util.PasswordHasher;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.time.LocalDateTime;
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
                "FROM users WHERE username = ? LIMIT 1";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password");

                    if (PasswordHasher.verify(plainPassword, storedHash)) {
                        int userId = rs.getInt("user_id");
                        String updateSql = "UPDATE users SET last_login = NOW(), is_active = 1 WHERE user_id = ?";
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setInt(1, userId);
                            updateStmt.executeUpdate();
                        }

                        User loggedInUser = mapResultSetToUser(rs);
                        loggedInUser.setActive(true);
                        return Optional.of(loggedInUser);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("UserDAO.login: DB Error - " + e.getMessage());
        }
        return Optional.empty();
    }

    // LOGOUT TO
    public OperationResult setAccountInactive(int userId) {
        String sql = "UPDATE users SET is_active = 0 WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            int rows = stmt.executeUpdate();
            return (rows > 0) ? OperationResult.SUCCESS : OperationResult.NOT_FOUND;

        } catch (SQLException e) {
            System.err.println("UserDAO.setAccountInactive: DB Error - " + e.getMessage());
            return OperationResult.DATABASE_ERROR;
        }
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
        String sql = "UPDATE users SET password = ? WHERE username = ? ";

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

    // PERMANENTLY DELETE USER FROM DATABASE
    public OperationResult permanentDeleteUser(int userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            int rows = stmt.executeUpdate();
            return (rows > 0) ? OperationResult.SUCCESS : OperationResult.NOT_FOUND;

        } catch (SQLException e) {
            System.err.println("UserDAO.permanentDeleteUser: DB Error - " + e.getMessage());
            return OperationResult.DATABASE_ERROR;
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("user_id"),
                rs.getString("username"),
                rs.getString("password"),
                AccountRole.fromString(rs.getString("role")),
                rs.getString("full_name"),
                rs.getBoolean("is_active")
        );
    }

    // PANG VERIFY SA FORGOT PASS IF EXISTING YUNG USER NA ADMIN
    public boolean verifyAdminCredentials(String adminUsername, String adminPassword) {
        String sql = "SELECT password, role FROM users WHERE username = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, adminUsername);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password");
                    String role = rs.getString("role");

                    if (PasswordHasher.verify(adminPassword, storedHash)) {
                        return "ADMIN".equalsIgnoreCase(role) ||
                                "MANAGER".equalsIgnoreCase(role) ||
                                "SUPERVISOR".equalsIgnoreCase(role);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("UserDAO.verifyAdminCredentials: DB Error - " + e.getMessage());
        }
        return false;
    }

    // PANG VERIFY SA FORGOT PASS KAHIT  INACTIVE YUNG USER
    public boolean checkUserExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ? LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("UserDAO.checkUserExists: DB Error - " + e.getMessage());
        }
        return false;
    }

    // SETTINGS ACCOUNT MANAGEMENT TO
    public List<AccountRow> getAllAccounts() {
        List<AccountRow> accounts = new ArrayList<>();
        String query = "SELECT user_id, full_name, username, role, is_active, last_login, created_at FROM users";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                long id = rs.getLong("user_id");
                String fullName = rs.getString("full_name");
                String username = rs.getString("username");

                AccountRole role = AccountRole.valueOf(rs.getString("role").toUpperCase());

                boolean isActive = rs.getBoolean("is_active");
                AccountStatus status = isActive ? AccountStatus.ACTIVE : AccountStatus.INACTIVE;

                LocalDateTime lastLogin = rs.getTimestamp("last_login") != null ?
                        rs.getTimestamp("last_login").toLocalDateTime() : null;

                // Converted to LocalDate to match your AccountRow model
                java.time.LocalDate createdAt = rs.getTimestamp("created_at") != null ?
                        rs.getTimestamp("created_at").toLocalDateTime().toLocalDate() : null;

                // Passed an empty string ("") for the email parameter to satisfy the compiler
                accounts.add(new AccountRow(id, fullName, username, role, status, lastLogin, createdAt));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return accounts;
    }

    public boolean updateAccount(int userId, String fullName, String username, String newPassword, AccountRole role, AccountStatus status) {
        boolean updatePassword = (newPassword != null && !newPassword.isEmpty());

        String sql;
        if (updatePassword) {
            sql = "UPDATE users SET full_name = ?, username = ?, password = ?, role = ?, is_active = ? WHERE user_id = ?";
        } else {
            sql = "UPDATE users SET full_name = ?, username = ?, role = ?, is_active = ? WHERE user_id = ?";
        }

        try (Connection conn = com.kainanresto.config.DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, fullName);
            pstmt.setString(2, username);

            int paramIndex = 3;

            if (updatePassword) {
                String hashedPassword = com.kainanresto.util.PasswordHasher.hash(newPassword);
                pstmt.setString(paramIndex++, hashedPassword);
            }

            pstmt.setString(paramIndex++, role.name());

            int isActive = (status == AccountStatus.ACTIVE) ? 1 : 0;
            pstmt.setInt(paramIndex++, isActive);
            pstmt.setInt(paramIndex, userId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error updating account details: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
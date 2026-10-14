package com.kainanresto.dao;

import com.kainanresto.config.DatabaseConfig;
import com.kainanresto.model.account.AccountRole;
import com.kainanresto.model.account.AccountRow;
import com.kainanresto.model.account.AccountStatus;
import com.kainanresto.model.account.User;
import com.kainanresto.util.PasswordHasher;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO {

    private static final int MYSQL_DUPLICATE_KEY_ERROR = 1062;

    private static final int USER_ID_MIN = 10_000_000;
    private static final int USER_ID_MAX = 99_999_999;
    private static final int USER_ID_MAX_ATTEMPTS = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    public enum OperationResult {
        SUCCESS,
        DUPLICATE_ENTRY,
        NOT_FOUND,
        DATABASE_ERROR
    }

    public Optional<User> login(String username, String plainPassword) {
        String sql = "SELECT user_id, username, password, role, full_name, is_active " +
                "FROM users WHERE username = ? AND is_deleted = 0 LIMIT 1";

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

    public OperationResult register(User user) {
        if (checkUserExists(user.getUsername())) {
            return OperationResult.DUPLICATE_ENTRY;
        }

        String sql = "INSERT INTO users (user_id, username, password, role, full_name, is_active, is_deleted) " +
                "VALUES (?, ?, ?, ?, ?, ?, 0)";
        String hashedPassword = PasswordHasher.hash(user.getPassword());

        for (int attempt = 0; attempt < USER_ID_MAX_ATTEMPTS; attempt++) {
            int randomId = generateRandomUserId();
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, randomId);
                stmt.setString(2, user.getUsername());
                stmt.setString(3, hashedPassword);
                stmt.setString(4, user.getRole().name());
                stmt.setString(5, user.getFullName());
                stmt.setBoolean(6, user.isActive());

                if (stmt.executeUpdate() > 0) {
                    user.setUserId(randomId);
                    return OperationResult.SUCCESS;
                }
                return OperationResult.DATABASE_ERROR;

            } catch (SQLIntegrityConstraintViolationException e) {
                if (e.getErrorCode() == MYSQL_DUPLICATE_KEY_ERROR) {
                    String msg = e.getMessage() == null ? "" : e.getMessage().toUpperCase();
                    if (msg.contains("PRIMARY")) continue;
                    return OperationResult.DUPLICATE_ENTRY;
                }
                System.err.println("UserDAO.register: DB Error - " + e.getMessage());
                return OperationResult.DATABASE_ERROR;
            } catch (SQLException e) {
                System.err.println("UserDAO.register: DB Error - " + e.getMessage());
                return OperationResult.DATABASE_ERROR;
            }
        }
        return OperationResult.DATABASE_ERROR;
    }

    private int generateRandomUserId() {
        return USER_ID_MIN + RANDOM.nextInt((USER_ID_MAX - USER_ID_MIN) + 1);
    }

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

    public OperationResult archiveUser(int userId) {
        String sql = "UPDATE users SET is_deleted = 1, is_active = 0 WHERE user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            int rows = stmt.executeUpdate();
            return (rows > 0) ? OperationResult.SUCCESS : OperationResult.NOT_FOUND;
        } catch (SQLException e) {
            System.err.println("UserDAO.archiveUser: DB Error - " + e.getMessage());
            return OperationResult.DATABASE_ERROR;
        }
    }

    public OperationResult restoreUser(int userId) {
        String sql = "UPDATE users SET is_deleted = 0 WHERE user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            int rows = stmt.executeUpdate();
            return (rows > 0) ? OperationResult.SUCCESS : OperationResult.NOT_FOUND;
        } catch (SQLException e) {
            System.err.println("UserDAO.restoreUser: DB Error - " + e.getMessage());
            return OperationResult.DATABASE_ERROR;
        }
    }

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

    public boolean verifyAdminCredentials(String adminUsername, String adminPassword) {
        String sql = "SELECT password, role FROM users WHERE username = ? AND is_deleted = 0";
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

    public List<AccountRow> getAllAccounts() {
        List<AccountRow> accounts = new ArrayList<>();
        String query = "SELECT user_id, full_name, username, role, is_active, last_login, created_at FROM users WHERE is_deleted = 0";

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
                LocalDateTime lastLogin = rs.getTimestamp("last_login") != null ? rs.getTimestamp("last_login").toLocalDateTime() : null;
                java.time.LocalDate createdAt = rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime().toLocalDate() : null;

                accounts.add(new AccountRow(id, fullName, username, role, status, lastLogin, createdAt));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return accounts;
    }

    public List<AccountRow> getArchivedAccounts() {
        List<AccountRow> accounts = new ArrayList<>();
        String query = "SELECT user_id, full_name, username, role, is_active, last_login, created_at FROM users WHERE is_deleted = 1";

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
                LocalDateTime lastLogin = rs.getTimestamp("last_login") != null ? rs.getTimestamp("last_login").toLocalDateTime() : null;
                java.time.LocalDate createdAt = rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime().toLocalDate() : null;

                accounts.add(new AccountRow(id, fullName, username, role, status, lastLogin, createdAt));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return accounts;
    }

    public boolean updateAccount(int userId, String fullName, String username, String newPassword, AccountRole role, AccountStatus status) {
        boolean updatePassword = (newPassword != null && !newPassword.isEmpty());
        String sql = updatePassword
                ? "UPDATE users SET full_name = ?, username = ?, password = ?, role = ?, is_active = ? WHERE user_id = ?"
                : "UPDATE users SET full_name = ?, username = ?, role = ?, is_active = ? WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, fullName);
            pstmt.setString(2, username);

            int paramIndex = 3;
            if (updatePassword) {
                pstmt.setString(paramIndex++, PasswordHasher.hash(newPassword));
            }

            pstmt.setString(paramIndex++, role.name());
            pstmt.setInt(paramIndex++, (status == AccountStatus.ACTIVE) ? 1 : 0);
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
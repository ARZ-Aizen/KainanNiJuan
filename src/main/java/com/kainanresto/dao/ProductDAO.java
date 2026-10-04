package com.kainanresto.dao;

import com.kainanresto.config.DatabaseConfig;
import com.kainanresto.controllers.main.admin.menu.MenuController.NewDishForm;
import com.kainanresto.model.dish.Dishes;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    /* ============================== IMAGE SERVER SETTINGS ============================== */

    // Server layout: http://192.168.1.15/kainan_images/<Category>/<filename>
    private static final String IMAGE_SERVER = "http://192.168.1.15/kainan_images/";
    private static final long RETRY_AFTER_MS = 30_000;   // retry the server after 30s if it was down
    private static volatile long serverDownUntil = 0;    // skip server checks until this time

    /* ============================== CATEGORIES ============================== */

    public List<String> getAllCategories() {
        List<String> categories = new ArrayList<>();
        String query = "SELECT name FROM categories ORDER BY name ASC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                categories.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categories;
    }

    public boolean addCategory(String categoryName) {
        String query = "INSERT INTO categories (name) VALUES (?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, categoryName);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteCategory(String categoryName) {
        String query = "DELETE FROM categories WHERE name = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, categoryName);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private int getCategoryIdByName(String categoryName, Connection conn) throws SQLException {
        String query = "SELECT id FROM categories WHERE name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, categoryName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return -1;
    }

    /* ============================== IMAGE URL RESOLVER ============================== */

    private static String encodePath(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String resolveImageUrl(String rawImageUri, String categoryName) {
        if (rawImageUri == null || rawImageUri.isEmpty()) {
            return null;
        }

        // Already a full URL stored in the DB? Use it as is.
        if (rawImageUri.startsWith("http://") || rawImageUri.startsWith("https://")) {
            return rawImageUri;
        }

        // Clean the filename
        String fileName = rawImageUri;
        if (fileName.contains("/") || fileName.contains("\\")) {
            fileName = fileName.substring(Math.max(fileName.lastIndexOf("/"), fileName.lastIndexOf("\\")) + 1);
        }

        // 1. Try the server first (skipped if it was recently unreachable)
        if (categoryName != null && System.currentTimeMillis() >= serverDownUntil) {
            HttpURLConnection connection = null;
            try {
                // Encode spaces (e.g., "banana cue.png" -> "banana%20cue.png")
                String serverUrl = IMAGE_SERVER + encodePath(categoryName) + "/" + encodePath(fileName);

                connection = (HttpURLConnection) new URL(serverUrl).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(1500);
                connection.setReadTimeout(1500);

                int code = connection.getResponseCode();

                if (code == 200) {
                    return serverUrl; // Image found on the server
                }
                // Server is up but file isn't there (404, etc.) -> fall through to local
            } catch (IOException e) {
                // Server unreachable -> don't retry for a while
                serverDownUntil = System.currentTimeMillis() + RETRY_AFTER_MS;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (connection != null) connection.disconnect();
            }
        }

        // 2. Fallback: local resources
        try {
            String resourcePath = "/com/kainanresto/images/main/PreloadDish/" + categoryName + "/" + fileName;
            URL localResource = getClass().getResource(resourcePath);

            if (localResource != null) {
                return localResource.toExternalForm();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 3. Last resort: whatever was stored
        return rawImageUri;
    }

    /* ============================== IMAGE UPLOAD ============================== */

    /** Sends the image to http://192.168.1.15/upload.php, which saves it in kainan_images/<Category>/. */
    public boolean uploadImage(File file, String categoryName) {
        HttpURLConnection conn = null;
        try {
            String url = "http://192.168.1.15/upload.php?category=" + encodePath(categoryName)
                    + "&filename=" + encodePath(file.getName());

            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(10000);
            conn.setFixedLengthStreamingMode(file.length());

            try (OutputStream out = conn.getOutputStream()) {
                Files.copy(file.toPath(), out);
            }

            int code = conn.getResponseCode();
            System.out.println("[UPLOAD] " + file.getName() + " -> HTTP " + code);
            return code == 200;
        } catch (Exception e) {
            System.out.println("[UPLOAD] failed: " + e);
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /* ============================== DISHES ============================== */

    public List<Dishes> getAllDishes() {
        List<Dishes> dishesList = new ArrayList<>();
        String query = """
            SELECT d.id, d.name, c.name AS category_name, d.price, d.image_url, d.available, d.description, d.quantity
            FROM dishes d
            JOIN categories c ON d.category_id = c.id
            ORDER BY d.name ASC
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                boolean isAvailable = rs.getBoolean("available") && rs.getInt("quantity") > 0;
                String categoryName = rs.getString("category_name");

                // Server first, then local resources, using the stored filename and category
                String validImageUrl = resolveImageUrl(rs.getString("image_url"), categoryName);

                Dishes dish = new Dishes(
                        rs.getLong("id"),
                        rs.getString("name"),
                        categoryName,
                        rs.getBigDecimal("price"),
                        validImageUrl,
                        isAvailable,
                        rs.getString("description"),
                        rs.getInt("quantity")
                );
                dishesList.add(dish);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return dishesList;
    }

    public boolean addDish(NewDishForm form) {
        String query = """
            INSERT INTO dishes (category_id, name, price, description, image_url, quantity, available) 
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConfig.getConnection()) {
            int categoryId = getCategoryIdByName(form.category(), conn);
            if (categoryId == -1) return false;

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, categoryId);
                stmt.setString(2, form.name());
                stmt.setBigDecimal(3, form.price());
                stmt.setString(4, form.description());
                stmt.setString(5, form.imageUri());
                stmt.setInt(6, form.quantity());
                stmt.setBoolean(7, form.available());

                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateDish(Dishes currentDish, NewDishForm form) {
        String query = """
            UPDATE dishes 
            SET category_id = ?, name = ?, price = ?, description = ?, image_url = ?, quantity = ?, available = ?
            WHERE id = ?
        """;

        try (Connection conn = DatabaseConfig.getConnection()) {
            int categoryId = getCategoryIdByName(form.category(), conn);
            if (categoryId == -1) return false;

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, categoryId);
                stmt.setString(2, form.name());
                stmt.setBigDecimal(3, form.price());
                stmt.setString(4, form.description());
                stmt.setString(5, form.imageUri());
                stmt.setInt(6, form.quantity());
                stmt.setBoolean(7, form.available());
                stmt.setLong(8, currentDish.id());

                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteDish(long id) {
        String query = "DELETE FROM dishes WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

}
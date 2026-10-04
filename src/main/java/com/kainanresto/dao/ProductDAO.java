package com.kainanresto.dao;

import com.kainanresto.config.DatabaseConfig;
import com.kainanresto.controllers.main.admin.menu.MenuController.NewDishForm;
import com.kainanresto.model.dish.Dishes;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

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

    private String resolveImageUrl(String rawImageUri, String categoryName) {
        if (rawImageUri == null || rawImageUri.isEmpty()) {
            return null;
        }

        // Clean the filename
        String fileName = rawImageUri;
        if (fileName.contains("/") || fileName.contains("\\")) {
            fileName = fileName.substring(Math.max(fileName.lastIndexOf("/"), fileName.lastIndexOf("\\")) + 1);
        }

        // 1. FASTEST METHOD: Try to load from your exact local resources path first!
        try {
            String resourcePath = "/com/kainanresto/images/main/PreloadDish/" + categoryName + "/" + fileName;
            URL localResource = getClass().getResource(resourcePath);

            if (localResource != null) {
                // Image found locally! Return it instantly (Handles spaces perfectly)
                return localResource.toExternalForm();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            // Encode spaces (e.g., "banana cue.png" -> "banana%20cue.png") for the web
            String safeFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            String serverUrl = "http://192.168.1.15/kainan_images/" + safeFileName;

            URL url = new URL(serverUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(800);
            connection.setReadTimeout(800);

            if (connection.getResponseCode() == 200) {
                return serverUrl; // Image found on the server
            }
        } catch (Exception e) {
            System.out.println("Image not found locally or on server: " + fileName);
        }

        return rawImageUri;
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

                // Pass both the stored URL and the Category Name to locate it in your resource folders
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
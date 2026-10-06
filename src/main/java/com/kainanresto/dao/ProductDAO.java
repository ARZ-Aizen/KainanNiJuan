package com.kainanresto.dao;

import com.kainanresto.config.DatabaseConfig;
import com.kainanresto.controllers.main.admin.menu.MenuController.NewDishForm;
import com.kainanresto.model.dish.Dishes;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProductDAO {

    /* ============================== IMAGE SETTINGS ============================== */

    // Change the host without editing code:  -Dkainan.server=192.168.1.20
    private static final String IMAGE_HOST   = System.getProperty("kainan.server", "192.168.1.15");
    private static final String IMAGE_SERVER = "http://" + IMAGE_HOST + "/kainan_images/";
    private static final String UPLOAD_URL   = "http://" + IMAGE_HOST + "/upload.php";

    // Optional folder on disk, same layout:  <folder>/<Category>/<filename>
    // Default: <user home>/kainan_images   (override with -Dkainan.localImages=D:/images)
    private static final Path LOCAL_IMAGE_DIR = Paths.get(
            System.getProperty("kainan.localImages", System.getProperty("user.home") + "/kainan_images"));

    private static final String RESOURCE_BASE = "/com/kainanresto/images/main/PreloadDish/";

    private static final long RETRY_AFTER_MS = 30_000;   // retry the server after 30s if it was down
    private static volatile long serverDownUntil = 0;    // skip server checks until this time
    private static final Map<String, String> serverHits = new ConcurrentHashMap<>();

    private static boolean debugged = false;


    private void debugClasspath(String category, String fileName) {
        if (debugged) return;
        debugged = true;
        String path = RESOURCE_BASE + category + "/" + fileName;
        System.out.println("[DEBUG] looking for: " + path);
        System.out.println("[DEBUG] found: " + getClass().getResource(path));
        System.out.println("[DEBUG] base folder: " + getClass().getResource(RESOURCE_BASE));
        System.out.println("[DEBUG] classes root: " + getClass().getResource("/"));
    }

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

    private static String fileNameOf(String raw) {
        String f = raw.replace("%20", " ");
        int i = Math.max(f.lastIndexOf('/'), f.lastIndexOf('\\'));
        return i >= 0 ? f.substring(i + 1) : f;
    }

    /** Order: server -> classpath -> local folder -> stored path (if it exists on this PC). */
    private String resolveImageUrl(String rawImageUri, String categoryName) {
        if (rawImageUri == null || rawImageUri.isBlank()) return null;
        String raw = rawImageUri.trim();

        // Already an http(s) URL? Use as is.
        if (raw.startsWith("http://") || raw.startsWith("https://")) return raw;

        String fileName = fileNameOf(raw);
        String cat = categoryName == null ? "" : categoryName;

        String url = findOnServer(cat, fileName);
        if (url == null) url = findInClasspath(cat, fileName);
        if (url == null) url = findInLocalFolder(cat, fileName);
        if (url == null) url = findStoredPath(raw);

        if (url == null) {
            System.out.println("[IMAGE] NOT FOUND: category='" + cat + "', file='" + fileName
                    + "' (tried server, " + RESOURCE_BASE + cat + "/, " + LOCAL_IMAGE_DIR + ")");
            return raw;
        }
        return url;
    }

    private String findOnServer(String category, String fileName) {
        if (category.isEmpty()) return null;

        String key = category + "/" + fileName;
        String cached = serverHits.get(key);
        if (cached != null) return cached;

        if (System.currentTimeMillis() < serverDownUntil) {
            System.out.println("[SERVER] skipped (marked down): " + key);
            return null;
        }

        String serverUrl = IMAGE_SERVER + encodePath(category) + "/" + encodePath(fileName);

        for (String method : new String[]{"HEAD", "GET"}) {
            HttpURLConnection c = null;
            try {
                c = (HttpURLConnection) new URL(serverUrl).openConnection();
                c.setRequestMethod(method);
                c.setConnectTimeout(2000);
                c.setReadTimeout(2000);

                int code = c.getResponseCode();
                if (code == 200) {
                    serverHits.put(key, serverUrl);
                    return serverUrl;
                }
                System.out.println("[SERVER] " + method + " " + code + " -> " + serverUrl);
                if (code != 405 && code != 501 && code != 403) return null; // real answer, no need to retry with GET
            } catch (IOException e) {
                System.out.println("[SERVER] unreachable (" + e + ") -> " + serverUrl);
                serverDownUntil = System.currentTimeMillis() + RETRY_AFTER_MS;
                return null;
            } finally {
                if (c != null) c.disconnect();
            }
        }
        return null;
    }

    private String findInClasspath(String category, String fileName) {
        debugClasspath(category, fileName);

        String[] candidates = {
                RESOURCE_BASE + category + "/" + fileName,
                RESOURCE_BASE + category + "/" + fileName.toLowerCase(),
                RESOURCE_BASE + fileName
        };
        for (String path : candidates) {
            URL res = getClass().getResource(path);
            if (res != null) return res.toExternalForm();
        }
        return null;
    }

    private String findInLocalFolder(String category, String fileName) {
        try {
            Path dir = LOCAL_IMAGE_DIR.resolve(category);
            Path exact = dir.resolve(fileName);
            if (Files.isRegularFile(exact)) return exact.toUri().toString();

            // case-insensitive match (Banana Shake.png vs banana shake.png)
            if (Files.isDirectory(dir)) {
                try (var stream = Files.list(dir)) {
                    return stream.filter(p -> p.getFileName().toString().equalsIgnoreCase(fileName))
                            .findFirst().map(p -> p.toUri().toString()).orElse(null);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Last resort: the path stored in the DB, only if it really exists on this machine. */
    private String findStoredPath(String raw) {
        try {
            File f;
            if (raw.startsWith("file:")) {
                f = new File(new URI(raw.replace(" ", "%20")));
            } else {
                f = new File(raw);
            }
            if (f.isFile()) return f.toURI().toString();
        } catch (Exception ignored) { }
        return null;
    }

    /* ============================== IMAGE UPLOAD ============================== */

    public boolean uploadImage(File file, String categoryName) {
        if (uploadToServer(file, categoryName)) return true;

        System.out.println("[UPLOAD] server failed, saving locally instead");
        return saveToLocalFolder(file, categoryName);
    }

    private boolean uploadToServer(File file, String categoryName) {
        if (System.currentTimeMillis() < serverDownUntil) return false;

        HttpURLConnection conn = null;
        try {
            String url = UPLOAD_URL + "?category=" + encodePath(categoryName)
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
            if (code == 200) {
                serverHits.clear();
                return true;
            }
            return false;
        } catch (IOException e) {
            System.out.println("[UPLOAD] server unreachable: " + e);
            serverDownUntil = System.currentTimeMillis() + RETRY_AFTER_MS;
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private boolean saveToLocalFolder(File file, String categoryName) {
        try {
            Path dir = LOCAL_IMAGE_DIR.resolve(categoryName);
            Files.createDirectories(dir);
            Path target = dir.resolve(file.getName());

            if (file.toPath().toAbsolutePath().normalize().equals(target.toAbsolutePath().normalize())) {
                return true;
            }
            Files.copy(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[UPLOAD] saved locally -> " + target);
            return true;
        } catch (Exception e) {
            System.out.println("[UPLOAD] local save failed: " + e);
            return false;
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
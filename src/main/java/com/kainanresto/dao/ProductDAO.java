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


    private static final String IMAGE_HOST    = System.getProperty("kainan.server", "192.168.1.15");
    private static final String BASE_URL      = "http://" + IMAGE_HOST + "/kainan/";
    private static final String IMAGE_SERVER  = BASE_URL + "kainan_images/";
    private static final String UPLOAD_URL    = BASE_URL + "upload.php";
    private static final String CATEGORY_URL  = BASE_URL + "make_category.php";
    private static final String DELETE_CATEGORY_URL = BASE_URL + "delete_category.php";
    private static final String DELETE_IMAGE_URL = BASE_URL + "delete_image.php";




    private static final Path LOCAL_IMAGE_DIR = Paths.get(
            System.getProperty("kainan.localImages", System.getProperty("user.home") + "/kainan_images"));

    private static final String RESOURCE_BASE = "/com/kainanresto/images/main/PreloadDish/";

    private static final long RETRY_AFTER_MS = 30_000;
    private static volatile long serverDownUntil = 0;
    private static final Map<String, String> serverHits = new ConcurrentHashMap<>();

    private List<String> categoryCache = null;

    public enum UploadResult { SERVER, LOCAL_ONLY, FAILED }

    // ======================= CATEGORIES =======================

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
            boolean ok = stmt.executeUpdate() > 0;
            if (ok) ensureCategoryFolder(categoryName);
            return ok;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean folderStillReferenced(String folder) {
        String q = "SELECT COUNT(*) FROM dishes WHERE image_url LIKE ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement st = conn.prepareStatement(q)) {
            st.setString(1, folder + "/%");
            try (ResultSet rs = st.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return true;
        }
    }

    public void deleteCategoryFolder(String categoryName) {
        String folder = safeFolder(categoryName);
        if (folder.isEmpty() || folderStillReferenced(folder)) return;

        deleteFolderOnServer(folder);
        deleteLocalFolder(folder);
        serverHits.clear();
    }

    private boolean deleteFolderOnServer(String folder) {
        if (System.currentTimeMillis() < serverDownUntil) return false;

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(DELETE_CATEGORY_URL + "?category=" + encodePath(folder)).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);
            return conn.getResponseCode() == 200;
        } catch (IOException e) {
            serverDownUntil = System.currentTimeMillis() + RETRY_AFTER_MS;
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private void deleteLocalFolder(String folder) {
        Path dir = LOCAL_IMAGE_DIR.resolve(folder);
        if (!Files.isDirectory(dir)) return;
        try (var walk = Files.walk(dir)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try { Files.delete(p); } catch (IOException ignored) { }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean deleteCategory(String categoryName) {
        String query = "DELETE FROM categories WHERE name = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, categoryName);
            boolean ok = stmt.executeUpdate() > 0;
            if (ok) deleteCategoryFolder(categoryName);
            return ok;
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

    // ======================= IMAGE HELPERS =======================

    private static String encodePath(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String safeFolder(String category) {
        return category == null ? "" : category.replaceAll("[\\\\/:*?\"<>|]", "").trim();
    }

    private static String fileNameOf(String raw) {
        String f = raw.replace("%20", " ");
        int i = Math.max(f.lastIndexOf('/'), f.lastIndexOf('\\'));
        return i >= 0 ? f.substring(i + 1) : f;
    }

    private String resolveImageUrl(String rawImageUri, String categoryName) {
        if (rawImageUri == null || rawImageUri.isBlank()) return null;
        String raw = rawImageUri.trim();

        if (raw.startsWith("http://") || raw.startsWith("https://")) return raw;

        String fileName = fileNameOf(raw);
        String cat = categoryName == null ? "" : categoryName;

        String url = findOnServer(cat, fileName);
        if (url == null) url = findInClasspath(cat, fileName);
        if (url == null) url = findInLocalFolder(cat, fileName);
        if (url == null) url = findStoredPath(raw);

        if (url == null) {
            List<String> others = categoryCache != null ? categoryCache : getAllCategories();
            for (String other : others) {
                if (other.equals(cat)) continue;
                url = findOnServer(other, fileName);
                if (url == null) url = findInClasspath(other, fileName);
                if (url == null) url = findInLocalFolder(other, fileName);
                if (url != null) break;
            }
        }

        return url == null ? raw : url;
    }

    private String findOnServer(String category, String fileName) {
        if (category.isEmpty()) return null;

        String key = category + "/" + fileName;
        String cached = serverHits.get(key);
        if (cached != null) return cached;

        if (System.currentTimeMillis() < serverDownUntil) return null;

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
                if (code != 405 && code != 501 && code != 403) return null;
            } catch (IOException e) {
                serverDownUntil = System.currentTimeMillis() + RETRY_AFTER_MS;
                return null;
            } finally {
                if (c != null) c.disconnect();
            }
        }
        return null;
    }

    private String findInClasspath(String category, String fileName) {
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

    public void ensureCategoryFolder(String categoryName) {
        String folder = safeFolder(categoryName);
        if (folder.isEmpty()) return;

        if (!createFolderOnServer(folder)) {
            try {
                Files.createDirectories(LOCAL_IMAGE_DIR.resolve(folder));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean createFolderOnServer(String folder) {
        if (System.currentTimeMillis() < serverDownUntil) return false;

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(CATEGORY_URL + "?category=" + encodePath(folder)).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);
            return conn.getResponseCode() == 200;
        } catch (IOException e) {
            serverDownUntil = System.currentTimeMillis() + RETRY_AFTER_MS;
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // ======================= UPLOAD =======================

    public boolean uploadImage(File file, String categoryName) {
        return uploadImageResult(file, categoryName) != UploadResult.FAILED;
    }

    public UploadResult uploadImageResult(File file, String categoryName) {
        if (uploadToServer(file, categoryName)) return UploadResult.SERVER;
        return saveToLocalFolder(file, categoryName) ? UploadResult.LOCAL_ONLY : UploadResult.FAILED;
    }

    private boolean uploadToServer(File file, String categoryName) {
        if (System.currentTimeMillis() < serverDownUntil) return false;

        HttpURLConnection conn = null;
        try {
            String url = UPLOAD_URL + "?category=" + encodePath(safeFolder(categoryName))
                    + "&filename=" + encodePath(file.getName());

            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(15000);
            conn.setFixedLengthStreamingMode(file.length());

            try (OutputStream out = conn.getOutputStream()) {
                Files.copy(file.toPath(), out);
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                serverHits.clear();
                return true;
            }
            return false;
        } catch (IOException e) {
            serverDownUntil = System.currentTimeMillis() + RETRY_AFTER_MS;
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private boolean saveToLocalFolder(File file, String categoryName) {
        try {
            Path dir = LOCAL_IMAGE_DIR.resolve(safeFolder(categoryName));
            Files.createDirectories(dir);
            Path target = dir.resolve(file.getName());

            if (file.toPath().toAbsolutePath().normalize().equals(target.toAbsolutePath().normalize())) {
                return true;
            }
            Files.copy(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ======================= DISHES =======================

    public List<Dishes> getAllDishes() {
        List<Dishes> dishesList = new ArrayList<>();
        categoryCache = getAllCategories();
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
        } finally {
            categoryCache = null;
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

            String oldImage = null;
            String oldCategory = null;
            String oldQuery = "SELECT d.image_url, c.name FROM dishes d JOIN categories c ON d.category_id = c.id WHERE d.id = ?";
            try (PreparedStatement s = conn.prepareStatement(oldQuery)) {
                s.setLong(1, currentDish.id());
                try (ResultSet rs = s.executeQuery()) {
                    if (rs.next()) {
                        oldImage = rs.getString(1);
                        oldCategory = rs.getString(2);
                    }
                }
            }

            boolean ok;
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, categoryId);
                stmt.setString(2, form.name());
                stmt.setBigDecimal(3, form.price());
                stmt.setString(4, form.description());
                stmt.setString(5, form.imageUri());
                stmt.setInt(6, form.quantity());
                stmt.setBoolean(7, form.available());
                stmt.setLong(8, currentDish.id());
                ok = stmt.executeUpdate() > 0;
            }

            if (ok && oldImage != null && !oldImage.isBlank()) {
                String newName = form.imageUri() == null ? "" : fileNameOf(form.imageUri());
                if (!fileNameOf(oldImage).equalsIgnoreCase(newName)) {
                    deleteImageIfUnused(conn, oldImage, oldCategory, currentDish.id());
                }
            }
            return ok;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteDish(long id) {
        String select = "SELECT d.image_url, c.name FROM dishes d JOIN categories c ON d.category_id = c.id WHERE d.id = ?";
        String delete = "DELETE FROM dishes WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection()) {
            String image = null;
            String category = null;
            try (PreparedStatement s = conn.prepareStatement(select)) {
                s.setLong(1, id);
                try (ResultSet rs = s.executeQuery()) {
                    if (rs.next()) {
                        image = rs.getString(1);
                        category = rs.getString(2);
                    }
                }
            }

            boolean ok;
            try (PreparedStatement stmt = conn.prepareStatement(delete)) {
                stmt.setLong(1, id);
                ok = stmt.executeUpdate() > 0;
            }

            if (ok) deleteImageIfUnused(conn, image, category, id);
            return ok;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void deleteImageIfUnused(Connection conn, String storedPath, String categoryName, long excludeDishId) {
        if (storedPath == null || storedPath.isBlank()) return;
        String raw = storedPath.trim();
        if (raw.startsWith("http") || raw.startsWith("file:") || raw.startsWith("jar:")) return;

        String normalized = raw.replace('\\', '/');
        String fileName = fileNameOf(normalized);
        String folder = normalized.contains("/")
                ? safeFolder(normalized.substring(0, normalized.lastIndexOf('/')))
                : safeFolder(categoryName);
        if (fileName.isBlank() || folder.isEmpty()) return;

        // keep the file if any other dish still points to it
        String q = "SELECT COUNT(*) FROM dishes WHERE id <> ? AND (image_url = ? OR image_url = ?)";
        try (PreparedStatement st = conn.prepareStatement(q)) {
            st.setLong(1, excludeDishId);
            st.setString(2, folder + "/" + fileName);
            st.setString(3, fileName);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;   // when in doubt, keep the image
        }

        deleteImageOnServer(folder, fileName);
        deleteLocalImage(folder, fileName);
        serverHits.remove(folder + "/" + fileName);
    }

    private boolean deleteImageOnServer(String folder, String fileName) {
        if (System.currentTimeMillis() < serverDownUntil) return false;

        HttpURLConnection conn = null;
        try {
            String url = DELETE_IMAGE_URL + "?category=" + encodePath(folder)
                    + "&filename=" + encodePath(fileName);
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);
            return conn.getResponseCode() == 200;
        } catch (IOException e) {
            serverDownUntil = System.currentTimeMillis() + RETRY_AFTER_MS;
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private void deleteLocalImage(String folder, String fileName) {
        try {
            Files.deleteIfExists(LOCAL_IMAGE_DIR.resolve(folder).resolve(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
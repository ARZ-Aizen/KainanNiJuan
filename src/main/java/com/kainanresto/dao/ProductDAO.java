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

    public static String getServerHost() {
        String sys = System.getProperty("kainan.server");
        if (sys != null && !sys.isBlank()) {
            return sys.trim();
        }
        java.util.Properties props = DatabaseConfig.getProperties();
        String host = props.getProperty("image.host",
                props.getProperty("server.host",
                props.getProperty("db.host", DatabaseConfig.getServerHost())));
        if (host != null && !host.isBlank()) {
            return host.trim();
        }
        return "localhost";
    }

    public static String getBaseUrl() {
        java.util.Properties props = DatabaseConfig.getProperties();
        String customUrl = props.getProperty("image.url");
        if (customUrl != null && !customUrl.isBlank()) {
            String u = customUrl.trim();
            return u.endsWith("/") ? u : u + "/";
        }
        String host = getServerHost();
        String port = props.getProperty("image.port", props.getProperty("http.port", "80")).trim();
        String hostPort = (port.isEmpty() || port.equals("80")) ? host : host + ":" + port;
        return "http://" + hostPort + "/kainan/";
    }

    public static String getImageServerUrl() {
        return getBaseUrl() + "kainan_images/";
    }

    public static String getUploadUrl() {
        return getBaseUrl() + "upload.php";
    }

    public static String getCategoryUrl() {
        return getBaseUrl() + "make_category.php";
    }

    public static String getDeleteCategoryUrl() {
        return getBaseUrl() + "delete_category.php";
    }

    public static String getDeleteImageUrl() {
        return getBaseUrl() + "delete_image.php";
    }

    private static final Path LOCAL_IMAGE_DIR = Paths.get(
            System.getProperty("kainan.localImages", System.getProperty("user.home") + "/kainan_images"));

    private static final String RESOURCE_BASE = "/com/kainanresto/images/main/PreloadDish/";


    public enum UploadResult {
        SERVER, LOCAL_ONLY, FAILED
    }

    public record ArchivedCategory(String name, int dishCount) {
    }

    static {
        ensureArchiveColumns();
    }

    private static void ensureArchiveColumns() {
        try (Connection conn = DatabaseConfig.getConnection();
                Statement stmt = conn.createStatement()) {
            try {
                stmt.executeUpdate("ALTER TABLE categories ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0");
            } catch (SQLException ignored) {
            }
            try {
                stmt.executeUpdate("ALTER TABLE dishes ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0");
            } catch (SQLException ignored) {
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getAllCategories() {
        List<String> categories = new ArrayList<>();
        String query = "SELECT name FROM categories WHERE is_deleted = 0 ORDER BY name ASC";

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
            if (ok)
                ensureCategoryFolder(categoryName);
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
        if (folder.isEmpty() || folderStillReferenced(folder))
            return;

        deleteFolderOnServer(folder);
        deleteLocalFolder(folder);
        com.kainanresto.util.DishImageLoader.clearCache();
    }

    private boolean deleteFolderOnServer(String folder) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(getDeleteCategoryUrl() + "?category=" + encodePath(folder))
                    .openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);
            return conn.getResponseCode() == 200;
        } catch (IOException e) {
            return false;
        } finally {
            if (conn != null)
                conn.disconnect();
        }
    }

    private void deleteLocalFolder(String folder) {
        Path dir = LOCAL_IMAGE_DIR.resolve(folder);
        if (!Files.isDirectory(dir))
            return;
        try (var walk = Files.walk(dir)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean deleteCategory(String categoryName) {
        return archiveCategory(categoryName);
    }

    public boolean archiveCategory(String categoryName) {
        String query = "UPDATE categories SET is_deleted = 1 WHERE name = ?";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, categoryName);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean restoreCategory(String categoryName) {
        String query = "UPDATE categories SET is_deleted = 0 WHERE name = ?";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, categoryName);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean permanentDeleteCategory(String categoryName) {
        String query = "DELETE FROM categories WHERE name = ?";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, categoryName);
            boolean ok = stmt.executeUpdate() > 0;
            if (ok)
                deleteCategoryFolder(categoryName);
            return ok;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<ArchivedCategory> getArchivedCategories() {
        List<ArchivedCategory> list = new ArrayList<>();
        String query = """
                    SELECT c.name, COUNT(d.id) AS dish_count
                    FROM categories c
                    LEFT JOIN dishes d ON c.id = d.category_id AND d.is_deleted = 0
                    WHERE c.is_deleted = 1
                    GROUP BY c.id, c.name
                    ORDER BY c.name ASC
                """;
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new ArchivedCategory(rs.getString("name"), rs.getInt("dish_count")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
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

    public static String resolveImageUrl(String rawImageUri, String categoryName) {
        if (rawImageUri == null || rawImageUri.isBlank())
            return null;
        String raw = rawImageUri.trim().replace('\\', '/');

        if (raw.startsWith("http://") || raw.startsWith("https://") || raw.startsWith("file:")) {
            return raw.replace(" ", "%20");
        }

        String fileName = fileNameOf(raw);
        String folderFromRaw = "";
        int slashIdx = raw.lastIndexOf('/');
        if (slashIdx >= 0) {
            folderFromRaw = raw.substring(0, slashIdx).trim();
        }

        String cat = categoryName == null ? "" : categoryName.trim();
        String targetFolder = !folderFromRaw.isEmpty() ? folderFromRaw : cat;

        // Quick check in classpath resources
        if (!targetFolder.isEmpty()) {
            String cp = findInClasspath(targetFolder, fileName);
            if (cp != null) return cp;
            String loc = findInLocalFolder(targetFolder, fileName);
            if (loc != null) return loc;
        }

        String stored = findStoredPath(raw);
        if (stored != null) return stored;

        // Direct HTTP URL on the image server
        if (!targetFolder.isEmpty() && !fileName.isEmpty()) {
            return getImageServerUrl() + encodePath(targetFolder) + "/" + encodePath(fileName);
        } else if (!fileName.isEmpty()) {
            return getImageServerUrl() + encodePath(fileName);
        }

        return raw;
    }

    private static String findInClasspath(String category, String fileName) {
        String[] candidates = {
                RESOURCE_BASE + category + "/" + fileName,
                RESOURCE_BASE + category + "/" + fileName.toLowerCase(),
                RESOURCE_BASE + fileName
        };
        for (String path : candidates) {
            URL res = ProductDAO.class.getResource(path);
            if (res != null)
                return res.toExternalForm();
        }
        return null;
    }

    private static String findInLocalFolder(String category, String fileName) {
        try {
            Path dir = LOCAL_IMAGE_DIR.resolve(category);
            Path exact = dir.resolve(fileName);
            if (Files.isRegularFile(exact))
                return exact.toUri().toString();

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

    private static String findStoredPath(String raw) {
        try {
            File f;
            if (raw.startsWith("file:")) {
                f = new File(new URI(raw.replace(" ", "%20")));
            } else {
                f = new File(raw);
            }
            if (f.isFile())
                return f.toURI().toString();
        } catch (Exception ignored) {
        }
        return null;
    }

    public void ensureCategoryFolder(String categoryName) {
        String folder = safeFolder(categoryName);
        if (folder.isEmpty())
            return;

        if (!createFolderOnServer(folder)) {
            try {
                Files.createDirectories(LOCAL_IMAGE_DIR.resolve(folder));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean createFolderOnServer(String folder) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(getCategoryUrl() + "?category=" + encodePath(folder)).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);
            return conn.getResponseCode() == 200;
        } catch (IOException e) {
            return false;
        } finally {
            if (conn != null)
                conn.disconnect();
        }
    }

    public boolean uploadImage(File file, String categoryName) {
        return uploadImageResult(file, categoryName) != UploadResult.FAILED;
    }

    public UploadResult uploadImageResult(File file, String categoryName) {
        if (uploadToServer(file, categoryName))
            return UploadResult.SERVER;
        return saveToLocalFolder(file, categoryName) ? UploadResult.LOCAL_ONLY : UploadResult.FAILED;
    }

    private boolean uploadToServer(File file, String categoryName) {
        HttpURLConnection conn = null;
        try {
            String url = getUploadUrl() + "?category=" + encodePath(safeFolder(categoryName))
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
                com.kainanresto.util.DishImageLoader.clearCache();
                return true;
            }
            return false;
        } catch (IOException e) {
            return false;
        } finally {
            if (conn != null)
                conn.disconnect();
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

    public List<Dishes> getAllDishes() {
        return queryDishes("WHERE d.is_deleted = 0 AND (c.is_deleted = 0 OR c.is_deleted IS NULL)");
    }

    public List<Dishes> getArchivedDishes() {
        return queryDishes("WHERE d.is_deleted = 1");
    }

    private List<Dishes> queryDishes(String whereClause) {
        List<Dishes> dishesList = new ArrayList<>();
        String query = """
                SELECT d.id, d.name, c.name AS category_name, d.price, d.image_url, d.available, d.description, d.quantity
                FROM dishes d
                LEFT JOIN categories c ON d.category_id = c.id
                """
                + " " + whereClause + " ORDER BY d.name ASC";

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
                        categoryName != null ? categoryName : "",
                        rs.getBigDecimal("price"),
                        validImageUrl,
                        isAvailable,
                        rs.getString("description"),
                        rs.getInt("quantity"));
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
            if (categoryId == -1)
                return false;

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
            if (categoryId == -1)
                return false;

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
        return archiveDish(id);
    }

    public boolean archiveDish(long id) {
        String query = "UPDATE dishes SET is_deleted = 1 WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean restoreDish(long id) {
        String query = "UPDATE dishes SET is_deleted = 0 WHERE id = ?";
        String restoreCat = "UPDATE categories SET is_deleted = 0 WHERE id = (SELECT category_id FROM dishes WHERE id = ?)";
        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(restoreCat)) {
                stmt.setLong(1, id);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setLong(1, id);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean permanentDeleteDish(long id) {
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

            if (ok)
                deleteImageIfUnused(conn, image, category, id);
            return ok;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void deleteImageIfUnused(Connection conn, String storedPath, String categoryName, long excludeDishId) {
        if (storedPath == null || storedPath.isBlank())
            return;
        String raw = storedPath.trim();
        if (raw.startsWith("http") || raw.startsWith("file:") || raw.startsWith("jar:"))
            return;

        String normalized = raw.replace('\\', '/');
        String fileName = fileNameOf(normalized);
        String folder = normalized.contains("/")
                ? safeFolder(normalized.substring(0, normalized.lastIndexOf('/')))
                : safeFolder(categoryName);
        if (fileName.isBlank() || folder.isEmpty())
            return;

        String q = "SELECT COUNT(*) FROM dishes WHERE id <> ? AND (image_url = ? OR image_url = ?)";
        try (PreparedStatement st = conn.prepareStatement(q)) {
            st.setLong(1, excludeDishId);
            st.setString(2, folder + "/" + fileName);
            st.setString(3, fileName);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0)
                    return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        deleteImageOnServer(folder, fileName);
        deleteLocalImage(folder, fileName);
        com.kainanresto.util.DishImageLoader.clearCache();
    }

    private boolean deleteImageOnServer(String folder, String fileName) {
        HttpURLConnection conn = null;
        try {
            String url = getDeleteImageUrl() + "?category=" + encodePath(folder)
                    + "&filename=" + encodePath(fileName);
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);
            return conn.getResponseCode() == 200;
        } catch (IOException e) {
            return false;
        } finally {
            if (conn != null)
                conn.disconnect();
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
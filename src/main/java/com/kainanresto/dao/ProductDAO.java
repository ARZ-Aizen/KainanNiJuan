    package com.kainanresto.dao;

    import com.kainanresto.config.DatabaseConfig;
    import com.kainanresto.controllers.main.admin.menu.MenuController.NewDishForm;
    import com.kainanresto.model.dish.Dishes;

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
            return -1; // Category not found
        }

        /* ============================== DISHES ============================== */

        public List<Dishes> getAllDishes() {
            List<Dishes> dishesList = new ArrayList<>();
            // FIX: Added d.description to the SELECT statement
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
                    // Determine actual availability based on the toggle AND the remaining quantity
                    boolean isAvailable = rs.getBoolean("available") && rs.getInt("quantity") > 0;

                    // FIX: Added description and quantity to match the updated record
                    Dishes dish = new Dishes(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getString("category_name"),
                            rs.getBigDecimal("price"),
                            rs.getString("image_url"),
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

        // FIX: Added the missing updateDish method called by your controller
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
                    stmt.setLong(8, currentDish.id()); // Target the existing row by ID

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
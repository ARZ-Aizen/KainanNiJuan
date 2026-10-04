package com.kainanresto.dao;

import com.kainanresto.config.DatabaseConfig;
import com.kainanresto.controllers.main.admin.dashboard.DashboardController.Range;
import com.kainanresto.controllers.main.admin.dashboard.DashboardData.BestSeller;
import com.kainanresto.controllers.main.admin.dashboard.DashboardData.ChartPoint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TransactionDAO {

    /* ============================== STAT CARDS ============================== */

    public BigDecimal getTodaysSales() {
        String sql = "SELECT COALESCE(SUM(total), 0) FROM orders WHERE status <> 'CANCELLED' AND created_at >= CURDATE()";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getBigDecimal(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getSalesDeltaPercent() {
        String sqlToday = "SELECT COALESCE(SUM(total), 0) FROM orders WHERE status <> 'CANCELLED' AND created_at >= CURDATE()";
        String sqlYesterday = "SELECT COALESCE(SUM(total), 0) FROM orders WHERE status <> 'CANCELLED' AND created_at >= CURDATE() - INTERVAL 1 DAY AND created_at < CURDATE()";

        try (Connection conn = DatabaseConfig.getConnection()) {
            BigDecimal today = BigDecimal.ZERO;
            BigDecimal yesterday = BigDecimal.ZERO;

            try (PreparedStatement ps = conn.prepareStatement(sqlToday); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) today = rs.getBigDecimal(1);
            }
            try (PreparedStatement ps = conn.prepareStatement(sqlYesterday); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) yesterday = rs.getBigDecimal(1);
            }

            if (yesterday.compareTo(BigDecimal.ZERO) == 0) return null;

            return today.subtract(yesterday)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(yesterday, 1, RoundingMode.HALF_UP);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int getTransactionsCountToday() {
        String sql = "SELECT COUNT(*) FROM orders WHERE created_at >= CURDATE()";
        return queryInt(sql);
    }

    public int getOrdersCountByStatusToday(String status) {
        String sql = "SELECT COUNT(*) FROM orders WHERE status = ? AND created_at >= CURDATE()";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getActivePreparingOrdersCount() {
        String sql = "SELECT COUNT(*) FROM orders WHERE status = 'PREPARING'";
        return queryInt(sql);
    }

    public int getTotalMenuItemsCount() {
        String sql = "SELECT COUNT(*) FROM dishes";
        return queryInt(sql);
    }

    public int getAvailableMenuItemsCount() {
        String sql = "SELECT COUNT(*) FROM dishes WHERE available = 1 AND quantity > 0";
        return queryInt(sql);
    }

    /* ============================== CHART ANALYTICS ============================== */

    public List<ChartPoint> getChartPoints(Range range) {
        List<ChartPoint> points = new ArrayList<>();
        String sql = switch (range) {
            case TODAY -> """
                SELECT HOUR(created_at) AS hr, COALESCE(SUM(total), 0) AS sales
                FROM orders
                WHERE status <> 'CANCELLED' AND created_at >= CURDATE()
                GROUP BY HOUR(created_at) ORDER BY hr ASC
            """;
            case WEEK -> """
                SELECT DATE(created_at) AS dt, COALESCE(SUM(total), 0) AS sales
                FROM orders
                WHERE status <> 'CANCELLED' AND created_at >= CURDATE() - INTERVAL 6 DAY
                GROUP BY DATE(created_at) ORDER BY dt ASC
            """;
            case MONTH -> """
                SELECT DATE(created_at) AS dt, COALESCE(SUM(total), 0) AS sales
                FROM orders
                WHERE status <> 'CANCELLED' AND created_at >= DATE_FORMAT(NOW(), '%Y-%m-01')
                GROUP BY DATE(created_at) ORDER BY dt ASC
            """;
        };

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (range == Range.TODAY) {
                Map<Integer, BigDecimal> hourSalesMap = new HashMap<>();
                while (rs.next()) {
                    hourSalesMap.put(rs.getInt("hr"), rs.getBigDecimal("sales"));
                }
                for (int h = 8; h <= 22; h += 2) { // 8 AM to 10 PM buckets
                    String label = (h % 12 == 0 ? 12 : h % 12) + (h < 12 ? " AM" : " PM");
                    BigDecimal val = hourSalesMap.getOrDefault(h, BigDecimal.ZERO);
                    points.add(new ChartPoint(label, val));
                }
            } else {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
                while (rs.next()) {
                    LocalDate dt = rs.getDate("dt").toLocalDate();
                    points.add(new ChartPoint(dt.format(fmt), rs.getBigDecimal("sales")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return points;
    }

    public int getRangeOrderCount(Range range) {
        String sql = switch (range) {
            case TODAY -> "SELECT COUNT(*) FROM orders WHERE status <> 'CANCELLED' AND created_at >= CURDATE()";
            case WEEK -> "SELECT COUNT(*) FROM orders WHERE status <> 'CANCELLED' AND created_at >= CURDATE() - INTERVAL 6 DAY";
            case MONTH -> "SELECT COUNT(*) FROM orders WHERE status <> 'CANCELLED' AND created_at >= DATE_FORMAT(NOW(), '%Y-%m-01')";
        };
        return queryInt(sql);
    }

    /* ============================== LOWER SECTION ============================== */

    public List<BestSeller> getTopBestSellers(int limit) {
        String sql = """
            SELECT oi.dish_name, SUM(oi.quantity) AS total_sold
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            WHERE o.status <> 'CANCELLED'
            GROUP BY oi.dish_name
            ORDER BY total_sold DESC
            LIMIT ?
        """;
        List<BestSeller> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new BestSeller(rs.getString("dish_name"), rs.getInt("total_sold")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /* ============================== SALES SUMMARY METHODS ============================== */

    public BigDecimal getWeeklySales() {
        String sql = "SELECT COALESCE(SUM(total), 0) FROM orders WHERE status <> 'CANCELLED' AND created_at >= CURDATE() - INTERVAL 6 DAY";
        return executeSalesQuery(sql);
    }

    public BigDecimal getMonthlySales() {
        String sql = "SELECT COALESCE(SUM(total), 0) FROM orders WHERE status <> 'CANCELLED' AND MONTH(created_at) = MONTH(CURDATE()) AND YEAR(created_at) = YEAR(CURDATE())";
        return executeSalesQuery(sql);
    }

    public BigDecimal getYearlySales() {
        String sql = "SELECT COALESCE(SUM(total), 0) FROM orders WHERE status <> 'CANCELLED' AND YEAR(created_at) = YEAR(CURDATE())";
        return executeSalesQuery(sql);
    }

    private BigDecimal executeSalesQuery(String sql) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                BigDecimal result = rs.getBigDecimal(1);
                return result != null ? result : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }

    private int queryInt(String sql) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
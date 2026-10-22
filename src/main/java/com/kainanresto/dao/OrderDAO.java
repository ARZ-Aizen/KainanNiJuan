package com.kainanresto.dao;

import com.kainanresto.config.DatabaseConfig;
import com.kainanresto.model.order.OrderReceipt;
import com.kainanresto.model.order.OrderStats;
import com.kainanresto.model.order.OrderStatus;
import com.kainanresto.model.order.SalesAnalyticsData;
import com.kainanresto.model.order.SalesAnalyticsData.SalesChartPoint;
import com.kainanresto.model.order.SalesPeriod;
import com.kainanresto.model.transac.PaymentResult;
import com.kainanresto.model.transac.ReceiptLine;
import com.kainanresto.model.transac.ReceiptTotals;
import com.kainanresto.model.transac.Transaction;

import java.math.BigDecimal;
import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.security.SecureRandom;

public class OrderDAO {

    private static final String ORDER_CODE_CHARS = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generateOrderNumber() {
        String check = "SELECT 1 FROM orders WHERE order_number = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(check)) {
            for (int attempt = 0; attempt < 20; attempt++) {
                StringBuilder sb = new StringBuilder(6);
                for (int i = 0; i < 6; i++) sb.append(ORDER_CODE_CHARS.charAt(RANDOM.nextInt(ORDER_CODE_CHARS.length())));
                String code = sb.toString();
                ps.setString(1, code);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return code;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Long.toString(System.currentTimeMillis() % 2_176_782_336L, 36).toUpperCase();
    }


    public boolean saveOrder(OrderReceipt r, PaymentResult pay, Integer cashierId, String cashierRole) {
        String insertOrder = """
            INSERT INTO orders (order_number, order_type, status, payment_status, created_at,
                cashier_id, cashier_name, cashier_role, discount_name, subtotal, discount_amount,
                service_charge_rate, service_charge, vat_rate, vat, total, cash_tendered, change_amount)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """;
        String insertItem = "INSERT INTO order_items (order_id, dish_id, dish_name, image_url, unit_price, quantity) VALUES (?,?,?,?,?,?)";
        String deduct = "UPDATE dishes SET quantity = quantity - ? WHERE id = ? AND quantity >= ?";

        ReceiptTotals t = r.totals();
        BigDecimal discountAmount = (r.discountName() == null || r.discountName().isBlank() || "None".equalsIgnoreCase(r.discountName()))
                ? BigDecimal.ZERO
                : t.subtotal().add(t.serviceCharge()).subtract(t.total());

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long orderId;
                try (PreparedStatement ps = conn.prepareStatement(insertOrder, Statement.RETURN_GENERATED_KEYS)) {
                    int i = 1;
                    ps.setString(i++, r.orderNumber());
                    ps.setString(i++, r.orderType());
                    ps.setString(i++, r.status().name());
                    ps.setString(i++, "Paid");
                    ps.setTimestamp(i++, Timestamp.valueOf(r.createdAt()));
                    if (cashierId == null) ps.setNull(i++, Types.INTEGER); else ps.setInt(i++, cashierId);
                    ps.setString(i++, r.cashierName());
                    ps.setString(i++, cashierRole);
                    ps.setString(i++, r.discountName());
                    ps.setBigDecimal(i++, t.subtotal());
                    ps.setBigDecimal(i++, discountAmount);
                    ps.setBigDecimal(i++, t.serviceChargeRate());
                    ps.setBigDecimal(i++, t.serviceCharge());
                    ps.setBigDecimal(i++, t.vatRate());
                    ps.setBigDecimal(i++, t.vat());
                    ps.setBigDecimal(i++, t.total());
                    ps.setBigDecimal(i++, pay.cashTendered());
                    ps.setBigDecimal(i, pay.change());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        orderId = keys.getLong(1);
                    }
                }

                try (PreparedStatement item = conn.prepareStatement(insertItem);
                     PreparedStatement stock = conn.prepareStatement(deduct)) {
                    for (ReceiptLine l : r.lines()) {
                        item.setLong(1, orderId);
                        item.setLong(2, l.dishId());
                        item.setString(3, l.name());
                        item.setString(4, l.imageUrl());
                        item.setBigDecimal(5, l.unitPrice());
                        item.setInt(6, l.quantity());
                        item.addBatch();

                        stock.setInt(1, l.quantity());
                        stock.setLong(2, l.dishId());
                        stock.setInt(3, l.quantity());
                        if (stock.executeUpdate() == 0) {
                            conn.rollback();
                            return false;
                        }
                    }
                    item.executeBatch();
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateStatus(String orderNumber, OrderStatus newStatus) {
        String update = "UPDATE orders SET status = ?, payment_status = ?, updated_at = NOW() WHERE order_number = ? AND status = 'PREPARING'";
        String items = "SELECT oi.dish_id, oi.quantity FROM order_items oi JOIN orders o ON o.id = oi.order_id WHERE o.order_number = ?";
        String restore = "UPDATE dishes SET quantity = quantity + ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(update)) {
                    ps.setString(1, newStatus.name());
                    ps.setString(2, newStatus == OrderStatus.CANCELLED ? "Refunded" : "Paid");
                    ps.setString(3, orderNumber);
                    if (ps.executeUpdate() == 0) { conn.rollback(); return false; }
                }
                if (newStatus == OrderStatus.CANCELLED) {
                    try (PreparedStatement sel = conn.prepareStatement(items);
                         PreparedStatement res = conn.prepareStatement(restore)) {
                        sel.setString(1, orderNumber);
                        try (ResultSet rs = sel.executeQuery()) {
                            while (rs.next()) {
                                res.setInt(1, rs.getInt("quantity"));
                                res.setLong(2, rs.getLong("dish_id"));
                                res.addBatch();
                            }
                        }
                        res.executeBatch();
                    }
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<OrderReceipt> loadReceipts(LocalDateTime since) {
        Map<Long, List<ReceiptLine>> linesByOrder = new HashMap<>();

        String linesSql = """
        SELECT oi.order_id, oi.dish_id, oi.dish_name, oi.image_url, oi.unit_price, oi.quantity
        FROM order_items oi 
        JOIN orders o ON o.id = oi.order_id 
        WHERE o.created_at >= ? OR o.status = 'PREPARING'
    """;

        String ordersSql = """
        SELECT id, order_number, order_type, status, created_at, cashier_name, discount_name,
               subtotal, service_charge_rate, service_charge, vat_rate, vat, total
        FROM orders 
        WHERE created_at >= ? OR status = 'PREPARING' 
        ORDER BY created_at DESC
    """;

        List<OrderReceipt> result = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(linesSql)) {
                ps.setTimestamp(1, Timestamp.valueOf(since));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String rawImg = rs.getString("image_url");
                        String resolvedImg = ProductDAO.resolveImageUrl(rawImg, null);
                        linesByOrder.computeIfAbsent(rs.getLong("order_id"), k -> new ArrayList<>())
                                .add(new ReceiptLine(rs.getLong("dish_id"), rs.getString("dish_name"),
                                        resolvedImg, rs.getBigDecimal("unit_price"), rs.getInt("quantity")));
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(ordersSql)) {
                ps.setTimestamp(1, Timestamp.valueOf(since));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ReceiptTotals totals = new ReceiptTotals(
                                rs.getBigDecimal("subtotal"),
                                rs.getBigDecimal("service_charge_rate"), rs.getBigDecimal("service_charge"),
                                rs.getBigDecimal("vat_rate"), rs.getBigDecimal("vat"),
                                rs.getBigDecimal("total"));
                        result.add(new OrderReceipt(
                                rs.getString("order_number"), rs.getString("order_type"),
                                OrderStatus.fromString(rs.getString("status")),
                                rs.getTimestamp("created_at").toLocalDateTime(),
                                rs.getString("cashier_name"),
                                linesByOrder.getOrDefault(rs.getLong("id"), List.of()),
                                rs.getString("discount_name"), totals));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public OrderReceipt findReceipt(String orderNumber) {
        String orderSql = """
        SELECT id, order_number, order_type, status, created_at, cashier_name, discount_name,
               subtotal, service_charge_rate, service_charge, vat_rate, vat, total
        FROM orders WHERE order_number = ?
    """;
        String linesSql = """
        SELECT dish_id, dish_name, image_url, unit_price, quantity
        FROM order_items WHERE order_id = ? ORDER BY id
    """;

        try (Connection conn = DatabaseConfig.getConnection()) {
            long orderId;
            OrderReceipt base;
            try (PreparedStatement ps = conn.prepareStatement(orderSql)) {
                ps.setString(1, orderNumber);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    orderId = rs.getLong("id");
                    ReceiptTotals totals = new ReceiptTotals(
                            rs.getBigDecimal("subtotal"),
                            rs.getBigDecimal("service_charge_rate"), rs.getBigDecimal("service_charge"),
                            rs.getBigDecimal("vat_rate"), rs.getBigDecimal("vat"),
                            rs.getBigDecimal("total"));
                    base = new OrderReceipt(
                            rs.getString("order_number"), rs.getString("order_type"),
                            OrderStatus.fromString(rs.getString("status")),
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getString("cashier_name"),
                            List.of(),
                            rs.getString("discount_name"), totals);
                }
            }

            List<ReceiptLine> lines = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(linesSql)) {
                ps.setLong(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String rawImg = rs.getString("image_url");
                        String resolvedImg = ProductDAO.resolveImageUrl(rawImg, null);
                        lines.add(new ReceiptLine(rs.getLong("dish_id"), rs.getString("dish_name"),
                                resolvedImg, rs.getBigDecimal("unit_price"), rs.getInt("quantity")));
                    }
                }
            }

            return new OrderReceipt(base.orderNumber(), base.orderType(), base.status(),
                    base.createdAt(), base.cashierName(), lines, base.discountName(), base.totals());
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public PaymentResult findPayment(String orderNumber) {
        String sql = "SELECT cash_tendered, change_amount FROM orders WHERE order_number = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                BigDecimal cash = rs.getBigDecimal("cash_tendered");
                BigDecimal change = rs.getBigDecimal("change_amount");
                return (cash == null || change == null) ? null : new PaymentResult(cash, change);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Transaction> getTransactions(LocalDateTime from, LocalDateTime to) {
        String sql = """
            SELECT order_number, order_type, cashier_name, cashier_role,
                   created_at, COALESCE(updated_at, created_at) AS last_activity,
                   total, status, payment_status
            FROM orders WHERE created_at >= ? AND created_at < ? ORDER BY created_at DESC
        """;
        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(from));
            ps.setTimestamp(2, Timestamp.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderStatus st = OrderStatus.fromString(rs.getString("status"));
                    list.add(new Transaction(
                            rs.getString("order_number"), rs.getString("order_type"), null,
                            rs.getString("cashier_name"), rs.getString("cashier_role"),
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getTimestamp("last_activity").toLocalDateTime(),
                            rs.getBigDecimal("total"),
                            st == null ? rs.getString("status") : st.getDisplayName(),
                            rs.getString("payment_status")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Transaction> getRecentTransactions(int limit) {
        String sql = """
            SELECT order_number, order_type, cashier_name, cashier_role,
                   created_at, COALESCE(updated_at, created_at) AS last_activity,
                   total, status, payment_status
            FROM orders ORDER BY created_at DESC LIMIT ?
        """;
        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderStatus st = OrderStatus.fromString(rs.getString("status"));
                    list.add(new Transaction(
                            rs.getString("order_number"), rs.getString("order_type"), null,
                            rs.getString("cashier_name"), rs.getString("cashier_role"),
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getTimestamp("last_activity").toLocalDateTime(),
                            rs.getBigDecimal("total"),
                            st == null ? rs.getString("status") : st.getDisplayName(),
                            rs.getString("payment_status")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public OrderStats getOrderStats() {
        LocalDate today = LocalDate.now();
        String sql = """
            SELECT COALESCE(SUM(CASE WHEN status <> 'CANCELLED' THEN total END), 0) AS revenue,
                   COUNT(*) AS total_orders,
                   COALESCE(SUM(status = 'CANCELLED'), 0) AS voids,
                   COALESCE(SUM(status = 'PREPARING'), 0) AS preparing,
                   COALESCE(AVG(CASE WHEN status = 'COMPLETED' THEN total END), 0) AS avg_spend
            FROM orders WHERE created_at >= ? AND created_at < ?
        """;
        try (Connection conn = DatabaseConfig.getConnection()) {
            BigDecimal revenue = BigDecimal.ZERO, avg = BigDecimal.ZERO;
            int total = 0, voids = 0, preparing = 0;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setTimestamp(1, Timestamp.valueOf(today.atStartOfDay()));
                ps.setTimestamp(2, Timestamp.valueOf(today.plusDays(1).atStartOfDay()));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        revenue = rs.getBigDecimal("revenue");
                        total = rs.getInt("total_orders");
                        voids = rs.getInt("voids");
                        preparing = rs.getInt("preparing");
                        avg = rs.getBigDecimal("avg_spend");
                    }
                }
            }

            LocalDate lastWeek = today.minusDays(7);
            BigDecimal prev = BigDecimal.ZERO;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(SUM(total),0) FROM orders WHERE status <> 'CANCELLED' AND created_at >= ? AND created_at < ?")) {
                ps.setTimestamp(1, Timestamp.valueOf(lastWeek.atStartOfDay()));
                ps.setTimestamp(2, Timestamp.valueOf(lastWeek.plusDays(1).atStartOfDay()));
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) prev = rs.getBigDecimal(1); }
            }
            String day = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            String revenueNote = prev.signum() == 0
                    ? "No sales last " + day
                    : String.format(Locale.ENGLISH, "%+.1f%% from last %s",
                    revenue.subtract(prev).multiply(BigDecimal.valueOf(100)).divide(prev, 1, java.math.RoundingMode.HALF_UP), day);

            return new OrderStats(revenue, revenueNote,
                    total, preparing + " active in kitchen",
                    voids, "Cancelled today",
                    avg, "Per completed transaction");
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public SalesAnalyticsData getSalesAnalytics(SalesPeriod period) {
        LocalDate today = LocalDate.now();
        LocalDateTime startDateTime;
        LocalDateTime endDateTime;
        String rangeLabel;

        switch (period) {
            case DAILY -> {
                startDateTime = today.atStartOfDay();
                endDateTime = today.plusDays(1).atStartOfDay();
                rangeLabel = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH));
            }
            case WEEKLY -> {
                LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate sunday = monday.plusDays(6);
                startDateTime = monday.atStartOfDay();
                endDateTime = sunday.plusDays(1).atStartOfDay();
                rangeLabel = monday.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)) + " - " +
                             sunday.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH));
            }
            case MONTHLY -> {
                LocalDate firstDay = today.withDayOfMonth(1);
                LocalDate lastDay = today.withDayOfMonth(today.lengthOfMonth());
                startDateTime = firstDay.atStartOfDay();
                endDateTime = lastDay.plusDays(1).atStartOfDay();
                rangeLabel = today.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));
            }
            case QUARTERLY -> {
                int currentMonth = today.getMonthValue();
                int qStartMonth = ((currentMonth - 1) / 3) * 3 + 1;
                int quarter = (currentMonth - 1) / 3 + 1;
                LocalDate qStart = LocalDate.of(today.getYear(), qStartMonth, 1);
                LocalDate qEnd = qStart.plusMonths(3).minusDays(1);
                startDateTime = qStart.atStartOfDay();
                endDateTime = qEnd.plusDays(1).atStartOfDay();
                rangeLabel = "Q" + quarter + " " + today.getYear() + " (" +
                             qStart.format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)) + " - " +
                             qEnd.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)) + ")";
            }
            case YEARLY -> {
                LocalDate yearStart = LocalDate.of(today.getYear(), 1, 1);
                LocalDate yearEnd = LocalDate.of(today.getYear(), 12, 31);
                startDateTime = yearStart.atStartOfDay();
                endDateTime = yearEnd.plusDays(1).atStartOfDay();
                rangeLabel = "Year " + today.getYear();
            }
            default -> {
                startDateTime = today.atStartOfDay();
                endDateTime = today.plusDays(1).atStartOfDay();
                rangeLabel = today.toString();
            }
        }

        BigDecimal profit = BigDecimal.ZERO;
        BigDecimal loss = BigDecimal.ZERO;
        BigDecimal grossSales = BigDecimal.ZERO;
        BigDecimal averageSpend = BigDecimal.ZERO;
        int totalOrdersCount = 0;
        int completedOrdersCount = 0;
        int cancelledOrdersCount = 0;
        int totalItemsSold = 0;

        String aggSql = """
            SELECT 
                COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN total ELSE 0 END), 0) AS profit,
                COALESCE(SUM(CASE WHEN status = 'CANCELLED' THEN total ELSE 0 END), 0) AS loss,
                COALESCE(SUM(CASE WHEN status <> 'CANCELLED' THEN total ELSE 0 END), 0) AS gross_sales,
                COALESCE(AVG(CASE WHEN status = 'COMPLETED' THEN total END), 0) AS avg_spend,
                COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) AS completed_orders,
                COUNT(CASE WHEN status = 'CANCELLED' THEN 1 END) AS cancelled_orders,
                COUNT(*) AS total_orders
            FROM orders
            WHERE created_at >= ? AND created_at < ?
        """;

        String itemsSoldSql = """
            SELECT COALESCE(SUM(oi.quantity), 0)
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            WHERE o.status = 'COMPLETED' AND o.created_at >= ? AND o.created_at < ?
        """;

        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(aggSql)) {
                ps.setTimestamp(1, Timestamp.valueOf(startDateTime));
                ps.setTimestamp(2, Timestamp.valueOf(endDateTime));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        profit = rs.getBigDecimal("profit");
                        loss = rs.getBigDecimal("loss");
                        grossSales = rs.getBigDecimal("gross_sales");
                        averageSpend = rs.getBigDecimal("avg_spend");
                        completedOrdersCount = rs.getInt("completed_orders");
                        cancelledOrdersCount = rs.getInt("cancelled_orders");
                        totalOrdersCount = rs.getInt("total_orders");
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(itemsSoldSql)) {
                ps.setTimestamp(1, Timestamp.valueOf(startDateTime));
                ps.setTimestamp(2, Timestamp.valueOf(endDateTime));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        totalItemsSold = rs.getInt(1);
                    }
                }
            }

            // Sales by order type
            Map<String, BigDecimal> salesByOrderType = new LinkedHashMap<>();
            String orderTypeSql = """
                SELECT order_type, COALESCE(SUM(total), 0) AS type_total
                FROM orders
                WHERE status = 'COMPLETED' AND created_at >= ? AND created_at < ?
                GROUP BY order_type
            """;
            try (PreparedStatement ps = conn.prepareStatement(orderTypeSql)) {
                ps.setTimestamp(1, Timestamp.valueOf(startDateTime));
                ps.setTimestamp(2, Timestamp.valueOf(endDateTime));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String type = rs.getString("order_type");
                        salesByOrderType.put(type == null ? "Standard" : type, rs.getBigDecimal("type_total"));
                    }
                }
            }

            // Chart points & Min/Max calculation
            List<SalesChartPoint> points = new ArrayList<>();
            String minDate = "—";
            BigDecimal minAmount = null;
            String maxDate = "—";
            BigDecimal maxAmount = null;

            if (period == SalesPeriod.DAILY) {
                String hourlySql = """
                    SELECT HOUR(created_at) AS hr, COALESCE(SUM(total), 0) AS sales, COUNT(*) AS cnt
                    FROM orders
                    WHERE status = 'COMPLETED' AND created_at >= ? AND created_at < ?
                    GROUP BY HOUR(created_at)
                """;
                Map<Integer, BigDecimal> salesByHour = new HashMap<>();
                Map<Integer, Integer> countByHour = new HashMap<>();
                try (PreparedStatement ps = conn.prepareStatement(hourlySql)) {
                    ps.setTimestamp(1, Timestamp.valueOf(startDateTime));
                    ps.setTimestamp(2, Timestamp.valueOf(endDateTime));
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            int hr = rs.getInt("hr");
                            salesByHour.put(hr, rs.getBigDecimal("sales"));
                            countByHour.put(hr, rs.getInt("cnt"));
                        }
                    }
                }
                for (int h = 8; h <= 22; h += 2) {
                    String label = (h % 12 == 0 ? 12 : h % 12) + (h < 12 ? " AM" : " PM");
                    BigDecimal val1 = salesByHour.getOrDefault(h, BigDecimal.ZERO);
                    BigDecimal val2 = salesByHour.getOrDefault(h + 1, BigDecimal.ZERO);
                    int c1 = countByHour.getOrDefault(h, 0);
                    int c2 = countByHour.getOrDefault(h + 1, 0);
                    BigDecimal totalH = val1.add(val2);
                    int totalC = c1 + c2;
                    points.add(new SalesChartPoint(label, totalH, totalC));
                    if (totalC > 0) {
                        if (maxAmount == null || totalH.compareTo(maxAmount) > 0) {
                            maxAmount = totalH;
                            maxDate = label;
                        }
                        if (minAmount == null || totalH.compareTo(minAmount) < 0) {
                            minAmount = totalH;
                            minDate = label;
                        }
                    }
                }
            } else if (period == SalesPeriod.WEEKLY) {
                String weeklySql = """
                    SELECT DATE(created_at) AS dt, COALESCE(SUM(total), 0) AS sales, COUNT(*) AS cnt
                    FROM orders
                    WHERE status = 'COMPLETED' AND created_at >= ? AND created_at < ?
                    GROUP BY DATE(created_at)
                """;
                Map<LocalDate, BigDecimal> salesByDay = new HashMap<>();
                Map<LocalDate, Integer> countByDay = new HashMap<>();
                try (PreparedStatement ps = conn.prepareStatement(weeklySql)) {
                    ps.setTimestamp(1, Timestamp.valueOf(startDateTime));
                    ps.setTimestamp(2, Timestamp.valueOf(endDateTime));
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            LocalDate dt = rs.getDate("dt").toLocalDate();
                            salesByDay.put(dt, rs.getBigDecimal("sales"));
                            countByDay.put(dt, rs.getInt("cnt"));
                        }
                    }
                }
                LocalDate monday = startDateTime.toLocalDate();
                DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("EEE (MMM d)", Locale.ENGLISH);
                for (int i = 0; i < 7; i++) {
                    LocalDate d = monday.plusDays(i);
                    String label = d.format(dayFmt);
                    BigDecimal s = salesByDay.getOrDefault(d, BigDecimal.ZERO);
                    int c = countByDay.getOrDefault(d, 0);
                    points.add(new SalesChartPoint(label, s, c));
                    if (c > 0) {
                        if (maxAmount == null || s.compareTo(maxAmount) > 0) {
                            maxAmount = s;
                            maxDate = d.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH));
                        }
                        if (minAmount == null || s.compareTo(minAmount) < 0) {
                            minAmount = s;
                            minDate = d.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH));
                        }
                    }
                }
            } else if (period == SalesPeriod.MONTHLY) {
                String monthlySql = """
                    SELECT DATE(created_at) AS dt, COALESCE(SUM(total), 0) AS sales, COUNT(*) AS cnt
                    FROM orders
                    WHERE status = 'COMPLETED' AND created_at >= ? AND created_at < ?
                    GROUP BY DATE(created_at)
                    ORDER BY dt ASC
                """;
                Map<LocalDate, BigDecimal> salesByDay = new HashMap<>();
                Map<LocalDate, Integer> countByDay = new HashMap<>();
                try (PreparedStatement ps = conn.prepareStatement(monthlySql)) {
                    ps.setTimestamp(1, Timestamp.valueOf(startDateTime));
                    ps.setTimestamp(2, Timestamp.valueOf(endDateTime));
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            LocalDate dt = rs.getDate("dt").toLocalDate();
                            salesByDay.put(dt, rs.getBigDecimal("sales"));
                            countByDay.put(dt, rs.getInt("cnt"));
                        }
                    }
                }
                LocalDate start = startDateTime.toLocalDate();
                int daysInMonth = start.lengthOfMonth();
                DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
                for (int i = 1; i <= daysInMonth; i++) {
                    LocalDate d = start.withDayOfMonth(i);
                    String label = String.valueOf(i);
                    BigDecimal s = salesByDay.getOrDefault(d, BigDecimal.ZERO);
                    int c = countByDay.getOrDefault(d, 0);
                    points.add(new SalesChartPoint(label, s, c));
                    if (c > 0) {
                        if (maxAmount == null || s.compareTo(maxAmount) > 0) {
                            maxAmount = s;
                            maxDate = d.format(dayFmt);
                        }
                        if (minAmount == null || s.compareTo(minAmount) < 0) {
                            minAmount = s;
                            minDate = d.format(dayFmt);
                        }
                    }
                }
            } else if (period == SalesPeriod.QUARTERLY) {
                String qSql = """
                    SELECT MONTH(created_at) AS m, COALESCE(SUM(total), 0) AS sales, COUNT(*) AS cnt
                    FROM orders
                    WHERE status = 'COMPLETED' AND created_at >= ? AND created_at < ?
                    GROUP BY MONTH(created_at)
                """;
                Map<Integer, BigDecimal> salesByMonth = new HashMap<>();
                Map<Integer, Integer> countByMonth = new HashMap<>();
                try (PreparedStatement ps = conn.prepareStatement(qSql)) {
                    ps.setTimestamp(1, Timestamp.valueOf(startDateTime));
                    ps.setTimestamp(2, Timestamp.valueOf(endDateTime));
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            salesByMonth.put(rs.getInt("m"), rs.getBigDecimal("sales"));
                            countByMonth.put(rs.getInt("m"), rs.getInt("cnt"));
                        }
                    }
                }
                int qStartMonth = startDateTime.getMonthValue();
                for (int i = 0; i < 3; i++) {
                    int m = qStartMonth + i;
                    String label = java.time.Month.of(m).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
                    BigDecimal s = salesByMonth.getOrDefault(m, BigDecimal.ZERO);
                    int c = countByMonth.getOrDefault(m, 0);
                    points.add(new SalesChartPoint(label, s, c));
                    if (c > 0) {
                        if (maxAmount == null || s.compareTo(maxAmount) > 0) {
                            maxAmount = s;
                            maxDate = label;
                        }
                        if (minAmount == null || s.compareTo(minAmount) < 0) {
                            minAmount = s;
                            minDate = label;
                        }
                    }
                }
            } else if (period == SalesPeriod.YEARLY) {
                String yrSql = """
                    SELECT MONTH(created_at) AS m, COALESCE(SUM(total), 0) AS sales, COUNT(*) AS cnt
                    FROM orders
                    WHERE status = 'COMPLETED' AND created_at >= ? AND created_at < ?
                    GROUP BY MONTH(created_at)
                """;
                Map<Integer, BigDecimal> salesByMonth = new HashMap<>();
                Map<Integer, Integer> countByMonth = new HashMap<>();
                try (PreparedStatement ps = conn.prepareStatement(yrSql)) {
                    ps.setTimestamp(1, Timestamp.valueOf(startDateTime));
                    ps.setTimestamp(2, Timestamp.valueOf(endDateTime));
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            salesByMonth.put(rs.getInt("m"), rs.getBigDecimal("sales"));
                            countByMonth.put(rs.getInt("m"), rs.getInt("cnt"));
                        }
                    }
                }
                for (int m = 1; m <= 12; m++) {
                    String label = java.time.Month.of(m).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                    BigDecimal s = salesByMonth.getOrDefault(m, BigDecimal.ZERO);
                    int c = countByMonth.getOrDefault(m, 0);
                    points.add(new SalesChartPoint(label, s, c));
                    if (c > 0) {
                        if (maxAmount == null || s.compareTo(maxAmount) > 0) {
                            maxAmount = s;
                            maxDate = java.time.Month.of(m).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
                        }
                        if (minAmount == null || s.compareTo(minAmount) < 0) {
                            minAmount = s;
                            minDate = java.time.Month.of(m).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
                        }
                    }
                }
            }

            int activeCount = 0;
            for (SalesChartPoint p : points) {
                if (p.sales().compareTo(BigDecimal.ZERO) > 0) activeCount++;
            }
            BigDecimal averageDaily = activeCount > 0
                    ? profit.divide(BigDecimal.valueOf(activeCount), 2, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            return new SalesAnalyticsData(
                    period,
                    rangeLabel,
                    profit,
                    loss,
                    grossSales,
                    averageSpend,
                    averageDaily,
                    totalOrdersCount,
                    completedOrdersCount,
                    cancelledOrdersCount,
                    totalItemsSold,
                    minDate,
                    minAmount == null ? BigDecimal.ZERO : minAmount,
                    maxDate,
                    maxAmount == null ? BigDecimal.ZERO : maxAmount,
                    points,
                    salesByOrderType
            );
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
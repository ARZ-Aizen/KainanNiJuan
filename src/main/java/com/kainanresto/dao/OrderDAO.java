package com.kainanresto.dao;

import com.kainanresto.config.DatabaseConfig;
import com.kainanresto.model.order.OrderReceipt;
import com.kainanresto.model.order.OrderStats;
import com.kainanresto.model.order.OrderStatus;
import com.kainanresto.model.transac.PaymentResult;
import com.kainanresto.model.transac.ReceiptLine;
import com.kainanresto.model.transac.ReceiptTotals;
import com.kainanresto.model.transac.Transaction;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.*;
import java.security.SecureRandom;

public class OrderDAO {

    /* ====================== SAVE (order + items + stock, one transaction) ====================== */

    private static final String ORDER_CODE_CHARS = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Short random order number (e.g. "7F3K9Q"), checked against the DB so it is unique. */
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
                    if (!rs.next()) return code;   // not taken
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // Fallback if the DB check fails: still short, still practically unique
        return Long.toString(System.currentTimeMillis() % 2_176_782_336L, 36).toUpperCase();
    }


    public boolean saveOrder(OrderReceipt r, PaymentResult pay, Integer cashierId, String cashierRole) {        String insertOrder = """
            INSERT INTO orders (order_number, order_type, order_type_detail, status, payment_status, created_at,
                cashier_id, cashier_name, cashier_role, discount_name, subtotal, discount_amount,
                service_charge_rate, service_charge, vat_rate, vat, total, cash_tendered, change_amount)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """;
        String insertItem = "INSERT INTO order_items (order_id, dish_id, dish_name, image_url, unit_price, quantity) VALUES (?,?,?,?,?,?)";
        String deduct = "UPDATE dishes SET quantity = quantity - ? WHERE id = ? AND quantity >= ?";

        ReceiptTotals t = r.totals();
        BigDecimal discountAmount = t.subtotal().add(t.serviceCharge()).add(t.vat()).subtract(t.total());

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long orderId;
                try (PreparedStatement ps = conn.prepareStatement(insertOrder, Statement.RETURN_GENERATED_KEYS)) {
                    int i = 1;
                    ps.setString(i++, r.orderNumber());
                    ps.setString(i++, r.orderType());
                    ps.setString(i++, r.orderTypeDetail());
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
                        if (stock.executeUpdate() == 0) {   // someone else sold the last ones
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

    /* ====================== STATUS CHANGE (cancel restores stock) ====================== */

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

    /* ====================== CLIENT: load orders into Order Management ====================== */

    public List<OrderReceipt> loadReceipts(LocalDateTime since) {
        Map<Long, List<ReceiptLine>> linesByOrder = new HashMap<>();

        // Updated: include items from today's orders OR any order currently PREPARING
        String linesSql = """
        SELECT oi.order_id, oi.dish_id, oi.dish_name, oi.image_url, oi.unit_price, oi.quantity
        FROM order_items oi 
        JOIN orders o ON o.id = oi.order_id 
        WHERE o.created_at >= ? OR o.status = 'PREPARING'
    """;

        // Updated: include today's orders OR any order currently PREPARING
        String ordersSql = """
        SELECT id, order_number, order_type, order_type_detail, status, created_at, cashier_name, discount_name,
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
                        linesByOrder.computeIfAbsent(rs.getLong("order_id"), k -> new ArrayList<>())
                                .add(new ReceiptLine(rs.getLong("dish_id"), rs.getString("dish_name"),
                                        rs.getString("image_url"), rs.getBigDecimal("unit_price"), rs.getInt("quantity")));
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
                                rs.getString("order_number"), rs.getString("order_type"), rs.getString("order_type_detail"),
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

    /** Loads one order (with its items) by order number, or null if not found. */
    public OrderReceipt findReceipt(String orderNumber) {
        String orderSql = """
        SELECT id, order_number, order_type, order_type_detail, status, created_at, cashier_name, discount_name,
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
                            rs.getString("order_number"), rs.getString("order_type"), rs.getString("order_type_detail"),
                            OrderStatus.fromString(rs.getString("status")),
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getString("cashier_name"),
                            List.of(),                       // filled in below
                            rs.getString("discount_name"), totals);
                }
            }

            List<ReceiptLine> lines = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(linesSql)) {
                ps.setLong(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        lines.add(new ReceiptLine(rs.getLong("dish_id"), rs.getString("dish_name"),
                                rs.getString("image_url"), rs.getBigDecimal("unit_price"), rs.getInt("quantity")));
                    }
                }
            }

            return new OrderReceipt(base.orderNumber(), base.orderType(), base.orderTypeDetail(), base.status(),
                    base.createdAt(), base.cashierName(), lines, base.discountName(), base.totals());
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Cash tendered and change saved with the order, or null if not available. */
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

    /* ====================== ADMIN: Sales & Orders table ====================== */

    public List<Transaction> getTransactions(LocalDateTime from, LocalDateTime to) {
        String sql = """
            SELECT order_number, order_type, cashier_name, cashier_role,
                   COALESCE(updated_at, created_at) AS last_activity, total, status, payment_status
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

    /** Stat cards are always for today. */
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

            // same weekday last week, for the revenue note
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
}
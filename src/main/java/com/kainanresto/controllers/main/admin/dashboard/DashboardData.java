package com.kainanresto.controllers.main.admin.dashboard;

import java.math.BigDecimal;
import java.util.List;

/**
 * Snapshot record for the admin dashboard.
 */
public record DashboardData(
        // Stat cards
        BigDecimal todaysSales,
        BigDecimal salesDeltaPercent,      // vs yesterday, e.g. 12.5 or -3.2 (null = unknown)
        int transactionsToday,
        int preparingOrders,               // shown under Transactions
        int completedToday,
        int cancelledToday,                // shown under Completed Orders
        int totalMenuItems,
        int availableMenuItems,

        // Chart for selected range (TODAY, WEEK, MONTH)
        List<ChartPoint> chartPoints,
        BigDecimal rangeTotalSales,
        int rangeOrdersCount,

        // Lower section cards
        List<BestSeller> bestSellers,      // volume ranked

        // Periodic sales summary (replacing inventory alerts)
        BigDecimal weeklySales,
        BigDecimal monthlySales,
        BigDecimal yearlySales
) {
    public DashboardData {
        chartPoints = chartPoints == null ? List.of() : List.copyOf(chartPoints);
        bestSellers = bestSellers == null ? List.of() : List.copyOf(bestSellers);
    }

    public record ChartPoint(String label, BigDecimal sales) {}
    public record BestSeller(String name, int quantitySold) {}
}
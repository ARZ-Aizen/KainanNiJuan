package com.kainanresto.controllers.main.admin.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record DashboardData(
        //DASHBOARD MAIN
        BigDecimal todaysSales,
        BigDecimal salesDeltaPercent,
        int transactionsToday,
        int preparingOrders,
        int completedToday,
        int cancelledToday,
        int totalMenuItems,
        int availableMenuItems,

        //STAT CHART
        List<ChartPoint> chartPoints,
        BigDecimal rangeTotalSales,
        int rangeOrdersCount,

        //BEST SELLER
        List<BestSeller> bestSellers,

        //SALES TO
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
package com.kainanresto.model.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record SalesAnalyticsData(
        SalesPeriod period,
        String periodRangeLabel,
        BigDecimal profit,               // Total Net Sales from Completed Orders
        BigDecimal loss,                 // Total Voids & Refunds from Cancelled Orders
        BigDecimal grossSales,           // Total Gross (completed + non-cancelled)
        BigDecimal averageSpend,         // Average Order Value (Profit / Completed Count)
        BigDecimal averageDailySales,    // Average Sales per day in period
        int totalOrdersCount,            // All transactions
        int completedOrdersCount,        // Completed count
        int cancelledOrdersCount,        // Voids / Refunds count
        int totalItemsSold,              // Units of food/drinks sold
        String minSalesDate,             // Minimum sales date label
        BigDecimal minSalesAmount,       // Minimum sales value
        String maxSalesDate,             // Maximum (peak) sales date label
        BigDecimal maxSalesAmount,       // Maximum (peak) sales value
        List<SalesChartPoint> chartPoints,
        Map<String, BigDecimal> salesByOrderType
) {
    public SalesAnalyticsData {
        chartPoints = chartPoints == null ? List.of() : List.copyOf(chartPoints);
        salesByOrderType = salesByOrderType == null ? Map.of() : Map.copyOf(salesByOrderType);
    }

    public record SalesChartPoint(String label, BigDecimal sales, int orderCount) {}
}

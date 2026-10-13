package com.kainanresto.controllers.main.admin.dashboard;

import com.kainanresto.controllers.main.admin.dashboard.DashboardController.Range;
import com.kainanresto.controllers.main.admin.dashboard.DashboardData.ChartPoint;
import com.kainanresto.dao.TransactionDAO;

import java.math.BigDecimal;
import java.util.List;

public class DashboardService {

    private final TransactionDAO transactionDAO = new TransactionDAO();

    public DashboardData fetchDashboardSnapshot(Range range) {
        //STAT CARD
        BigDecimal todaysSales = transactionDAO.getTodaysSales();
        BigDecimal salesDelta = transactionDAO.getSalesDeltaPercent();
        int transactionsToday = transactionDAO.getTransactionsCountToday();
        int preparingOrders = transactionDAO.getActivePreparingOrdersCount();
        int completedToday = transactionDAO.getOrdersCountByStatusToday("COMPLETED");
        int cancelledToday = transactionDAO.getOrdersCountByStatusToday("CANCELLED");
        int totalMenuItems = transactionDAO.getTotalMenuItemsCount();
        int availableMenuItems = transactionDAO.getAvailableMenuItemsCount();

        //SA ANALYTICS
        List<ChartPoint> chartPoints = transactionDAO.getChartPoints(range);
        BigDecimal rangeTotalSales = chartPoints.stream()
                .map(ChartPoint::sales)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int rangeOrdersCount = transactionDAO.getRangeOrderCount(range);

        //BEST SELLER
        var bestSellers = transactionDAO.getTopBestSellers(5);

        //SALES SUMMARY
        BigDecimal weeklySales = transactionDAO.getWeeklySales();
        BigDecimal monthlySales = transactionDAO.getMonthlySales();
        BigDecimal yearlySales = transactionDAO.getYearlySales();

        return new DashboardData(
                todaysSales, salesDelta, transactionsToday, preparingOrders,
                completedToday, cancelledToday, totalMenuItems, availableMenuItems,
                chartPoints, rangeTotalSales, rangeOrdersCount,
                bestSellers, weeklySales, monthlySales, yearlySales
        );
    }
}
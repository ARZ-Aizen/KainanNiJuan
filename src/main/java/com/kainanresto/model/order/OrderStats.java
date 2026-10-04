package com.kainanresto.model.order;

import java.math.BigDecimal;

public record OrderStats(
        BigDecimal todaysRevenue,   String todaysRevenueNote,
        int totalOrders,            String totalOrdersNote,
        int voidsAndRefunds,        String voidsAndRefundsNote,
        BigDecimal averageSpend,    String averageSpendNote
) {}
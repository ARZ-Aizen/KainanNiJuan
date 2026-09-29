package com.kainanresto.model;

import java.math.BigDecimal;

/** Stat-card values for Sales & Orders. Note strings are pre-formatted by the server. */
public record OrderStats(
        BigDecimal todaysRevenue,   String todaysRevenueNote,
        int totalOrders,            String totalOrdersNote,
        int voidsAndRefunds,        String voidsAndRefundsNote,
        BigDecimal averageSpend,    String averageSpendNote
) {}
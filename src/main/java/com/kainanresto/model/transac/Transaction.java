package com.kainanresto.model.transac;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One row of the Sales & Orders table. Populated by the server/database layer. */
public record Transaction(
        String orderId,
        String orderType,
        String customerName,      // searchable, not shown in the table
        String staffName,
        String staffRole,
        LocalDateTime lastActivityAt,
        BigDecimal total,
        String status,            // e.g. Pending / Preparing / Completed / Cancelled
        String paymentStatus      // e.g. Paid / Unpaid / Refunded
) {}
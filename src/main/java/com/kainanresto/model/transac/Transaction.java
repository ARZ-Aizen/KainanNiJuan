package com.kainanresto.model.transac;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Transaction(
        String orderId,
        String orderType,
        String customerName,
        String staffName,
        String staffRole,
        LocalDateTime createdAt,
        LocalDateTime lastActivityAt,
        BigDecimal total,
        String status,
        String paymentStatus
) {
    public Transaction(String orderId, String orderType, String customerName,
                       String staffName, String staffRole, LocalDateTime lastActivityAt,
                       BigDecimal total, String status, String paymentStatus) {
        this(orderId, orderType, customerName, staffName, staffRole,
                lastActivityAt, lastActivityAt, total, status, paymentStatus);
    }
}
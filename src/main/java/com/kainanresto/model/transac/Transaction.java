    package com.kainanresto.model.transac;

    import java.math.BigDecimal;
    import java.time.LocalDateTime;

    public record Transaction(
            String orderId,
            String orderType,
            String customerName,
            String staffName,
            String staffRole,
            LocalDateTime lastActivityAt,
            BigDecimal total,
            String status,
            String paymentStatus
    ) {}
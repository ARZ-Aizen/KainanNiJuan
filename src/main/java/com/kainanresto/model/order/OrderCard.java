package com.kainanresto.model.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderCard(String orderNumber, String orderType,
                        OrderStatus status, LocalDateTime createdAt,
                        List<OrderLine> lines, BigDecimal total) {}
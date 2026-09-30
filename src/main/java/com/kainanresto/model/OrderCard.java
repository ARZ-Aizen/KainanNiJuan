package com.kainanresto.model;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderCard(String orderNumber, String orderType, String orderTypeDetail,
                        OrderStatus status, LocalDateTime createdAt,
                        List<OrderLine> lines, BigDecimal total) {}
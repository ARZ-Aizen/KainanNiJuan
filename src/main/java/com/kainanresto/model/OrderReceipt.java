package com.kainanresto.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full detail of one order for the Order Management receipt panel.
 * discountName may be null (shown as "None"); the discount is already reflected in totals.total().
 */
public record OrderReceipt(String orderNumber,
                           String orderType,
                           String orderTypeDetail,
                           OrderStatus status,
                           LocalDateTime createdAt,
                           String cashierName,
                           List<ReceiptLine> lines,
                           String discountName,
                           ReceiptTotals totals) { }
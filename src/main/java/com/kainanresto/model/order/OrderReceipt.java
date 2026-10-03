package com.kainanresto.model.order;

import com.kainanresto.model.transac.ReceiptLine;
import com.kainanresto.model.transac.ReceiptTotals;

import java.time.LocalDateTime;
import java.util.List;

public record OrderReceipt(String orderNumber,
                           String orderType,
                           String orderTypeDetail,
                           OrderStatus status,
                           LocalDateTime createdAt,
                           String cashierName,
                           List<ReceiptLine> lines,
                           String discountName,
                           ReceiptTotals totals) { }
package com.kainanresto.model.transac;

import java.math.BigDecimal;

public record ReceiptLine(long dishId, String name, String imageUrl, BigDecimal unitPrice, int quantity) {
    public BigDecimal lineTotal() {
        return unitPrice == null ? BigDecimal.ZERO : unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
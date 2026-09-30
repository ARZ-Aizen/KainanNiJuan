// ReceiptLine.java
package com.kainanresto.model.transac;

import java.math.BigDecimal;

public record ReceiptLine(String name, String imageUrl, BigDecimal unitPrice, int quantity) {
    /** Computed, not stored: unit price x quantity (null when there is no price). */
    public BigDecimal lineTotal() {
        return unitPrice == null ? null : unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
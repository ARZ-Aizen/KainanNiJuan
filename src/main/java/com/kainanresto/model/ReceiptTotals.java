// ReceiptTotals.java
package com.kainanresto.model;

import java.math.BigDecimal;

/** Rates are percentages (5 = 5%). The discount is already reflected in total. */
public record ReceiptTotals(BigDecimal subtotal,
                            BigDecimal serviceChargeRate, BigDecimal serviceCharge,
                            BigDecimal vatRate, BigDecimal vat,
                            BigDecimal total) { }
package com.kainanresto.model.transac;

import java.math.BigDecimal;

public record ReceiptTotals(BigDecimal subtotal,
                            BigDecimal serviceChargeRate, BigDecimal serviceCharge,
                            BigDecimal vatRate, BigDecimal vat,
                            BigDecimal total) { }
package com.kainanresto.model.transac;

import java.math.BigDecimal;

public record PaymentResult(BigDecimal cashTendered, BigDecimal change) { }

package com.kainanresto.model.transac;

import java.math.BigDecimal;

/** Outcome of a cash payment: what the customer handed over and the change returned. */
public record PaymentResult(BigDecimal cashTendered, BigDecimal change) { }

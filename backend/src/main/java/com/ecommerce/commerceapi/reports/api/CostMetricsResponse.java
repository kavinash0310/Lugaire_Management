package com.ecommerce.commerceapi.reports.api;

import java.math.BigDecimal;

public record CostMetricsResponse(
        BigDecimal productCost,
        BigDecimal returnLoss,
        BigDecimal marketplaceFees,
        BigDecimal shippingCharges,
        BigDecimal returnCharges,
        BigDecimal otherCharges,
        BigDecimal settlementReceived,
        BigDecimal settlementDifference) {}

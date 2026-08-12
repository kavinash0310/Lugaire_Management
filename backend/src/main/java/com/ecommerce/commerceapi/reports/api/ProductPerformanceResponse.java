package com.ecommerce.commerceapi.reports.api;

import java.math.BigDecimal;

public record ProductPerformanceResponse(
        Long productId,
        String productName,
        String sku,
        long unitsSold,
        BigDecimal revenue,
        BigDecimal productCost,
        BigDecimal returnsLoss,
        BigDecimal profit) {}

package com.ecommerce.commerceapi.reports.api;

import java.math.BigDecimal;

public record MarketplacePerformanceResponse(
        String platform,
        long orders,
        BigDecimal sales,
        BigDecimal marketplaceFees,
        BigDecimal shippingCharges,
        BigDecimal returnCharges,
        BigDecimal settlements,
        BigDecimal netRevenue,
        BigDecimal profit) {}

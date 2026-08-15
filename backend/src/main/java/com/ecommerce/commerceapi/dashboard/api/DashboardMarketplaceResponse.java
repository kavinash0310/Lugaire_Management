package com.ecommerce.commerceapi.dashboard.api;

import java.math.BigDecimal;

public record DashboardMarketplaceResponse(
        String platform,
        long orders,
        BigDecimal sales,
        long returnsRto,
        BigDecimal marketplaceFees,
        BigDecimal shippingCharges,
        BigDecimal returnCharges,
        BigDecimal settlements,
        BigDecimal netRevenue,
        BigDecimal profit) {
}

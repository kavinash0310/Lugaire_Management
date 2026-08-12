package com.ecommerce.commerceapi.reports.api;

import java.math.BigDecimal;

public record RevenueMetricsResponse(
        BigDecimal grossSales,
        BigDecimal deliveredSales,
        BigDecimal returnedSales,
        BigDecimal rtoSales,
        BigDecimal cancelledSales,
        long orders,
        long deliveredOrders,
        long returnedOrders,
        long rtoOrders,
        long cancelledOrders) {}

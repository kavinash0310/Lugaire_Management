package com.ecommerce.commerceapi.dashboard.api;

public record DashboardBacklogResponse(
        long pendingPayments,
        long pendingSettlements) {
}

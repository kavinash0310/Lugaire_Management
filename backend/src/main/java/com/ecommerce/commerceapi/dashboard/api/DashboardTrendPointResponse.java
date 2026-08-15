package com.ecommerce.commerceapi.dashboard.api;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardTrendPointResponse(
        LocalDate date,
        BigDecimal value,
        long count) {
}

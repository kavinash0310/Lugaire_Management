package com.ecommerce.commerceapi.reports.api;

import java.math.BigDecimal;

public record ProfitMetricsResponse(BigDecimal grossProfit, BigDecimal netProfit, BigDecimal profitMargin) {}

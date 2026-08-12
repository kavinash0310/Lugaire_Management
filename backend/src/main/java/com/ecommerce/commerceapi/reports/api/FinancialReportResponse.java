package com.ecommerce.commerceapi.reports.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record FinancialReportResponse(
        String period,
        LocalDate fromDate,
        LocalDate toDate,
        RevenueMetricsResponse revenue,
        CostMetricsResponse costs,
        ExpenseSummaryResponse expenses,
        ProfitMetricsResponse profit,
        List<MarketplacePerformanceResponse> marketplaces,
        List<ExpenseBreakdownResponse> expenseBreakdown,
        List<ProductPerformanceResponse> products,
        Instant generatedAt) {}

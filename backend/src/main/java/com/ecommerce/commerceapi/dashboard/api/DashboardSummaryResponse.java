package com.ecommerce.commerceapi.dashboard.api;

import com.ecommerce.commerceapi.inventory.api.InventorySummaryResponse;
import com.ecommerce.commerceapi.reports.api.CostMetricsResponse;
import com.ecommerce.commerceapi.reports.api.ExpenseBreakdownResponse;
import com.ecommerce.commerceapi.reports.api.ExpenseSummaryResponse;
import com.ecommerce.commerceapi.reports.api.ProfitMetricsResponse;
import com.ecommerce.commerceapi.reports.api.RevenueMetricsResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DashboardSummaryResponse(
        String businessName,
        String currencySymbol,
        String period,
        LocalDate fromDate,
        LocalDate toDate,
        RevenueMetricsResponse revenue,
        CostMetricsResponse costs,
        ExpenseSummaryResponse expenses,
        ProfitMetricsResponse profit,
        DashboardBacklogResponse backlog,
        ReturnsSummaryResponse returns,
        List<DashboardTrendPointResponse> salesTrend,
        List<DashboardTrendPointResponse> profitTrend,
        List<DashboardStatusResponse> orderStatusBreakdown,
        List<DashboardMarketplaceResponse> marketplaces,
        List<InventorySummaryResponse> lowStock,
        List<ExpenseBreakdownResponse> expenseBreakdown,
        Instant generatedAt) {

    public record ReturnsSummaryResponse(
            long returnCount,
            long rtoCount,
            BigDecimal returnRate,
            BigDecimal rtoRate) {
    }
}

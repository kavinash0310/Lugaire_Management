package com.ecommerce.commerceapi.reports.api;

import java.math.BigDecimal;

public record ExpenseSummaryResponse(BigDecimal totalExpenses, BigDecimal paidExpenses, BigDecimal pendingExpenses) {}

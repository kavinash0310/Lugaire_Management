package com.ecommerce.commerceapi.reports.api;

import java.math.BigDecimal;

public record ExpenseBreakdownResponse(Long categoryId, String categoryName, BigDecimal amount, BigDecimal percentage) {}

package com.ecommerce.commerceapi.expenses.api;

import java.util.List;

public record ExpensePageResponse(List<ExpenseResponse> content, int page, int size, long totalElements, int totalPages) {}

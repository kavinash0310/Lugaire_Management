package com.ecommerce.commerceapi.expenses.api;

import com.ecommerce.commerceapi.expenses.domain.ExpensePaymentStatus;
import jakarta.validation.constraints.NotNull;

public record ExpensePaymentStatusRequest(@NotNull ExpensePaymentStatus paymentStatus) {}

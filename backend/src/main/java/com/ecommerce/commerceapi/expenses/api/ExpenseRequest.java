package com.ecommerce.commerceapi.expenses.api;

import com.ecommerce.commerceapi.expenses.domain.ExpensePaymentMethod;
import com.ecommerce.commerceapi.expenses.domain.ExpensePaymentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequest(
        @NotNull LocalDate expenseDate,
        @NotNull Long categoryId,
        Long supplierId,
        @NotBlank String description,
        @NotNull @DecimalMin("0") BigDecimal amount,
        @NotNull @DecimalMin("0") BigDecimal taxAmount,
        @NotNull @DecimalMin("0") BigDecimal totalAmount,
        @NotNull ExpensePaymentMethod paymentMethod,
        @NotNull ExpensePaymentStatus paymentStatus,
        String referenceNumber,
        String remarks) {}

package com.ecommerce.commerceapi.expenses.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ExpenseResponse(
        Long id,
        String expenseNumber,
        LocalDate expenseDate,
        Long categoryId,
        String categoryName,
        Long supplierId,
        String supplierName,
        String description,
        BigDecimal amount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String paymentMethod,
        String paymentStatus,
        String referenceNumber,
        String remarks,
        Instant createdAt,
        Instant updatedAt) {}

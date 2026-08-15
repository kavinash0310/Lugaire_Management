package com.ecommerce.commerceapi.inventory.api;

import com.ecommerce.commerceapi.inventory.domain.InventoryTransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record InventoryTransactionRequest(
        @NotNull Long variantId,
        @NotNull InventoryTransactionType transactionType,
        @Positive int quantity,
        @DecimalMin("0") BigDecimal unitCost,
        String referenceType,
        String referenceId,
        String notes,
        String remarks,
        @NotNull LocalDate transactionDate) {
}

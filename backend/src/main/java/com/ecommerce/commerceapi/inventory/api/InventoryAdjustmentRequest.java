package com.ecommerce.commerceapi.inventory.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record InventoryAdjustmentRequest(
        @NotNull Long variantId,
        @NotNull InventoryAdjustmentType adjustmentType,
        @Positive int quantity,
        @NotBlank String reason,
        String remarks,
        @NotNull LocalDate transactionDate) {
}

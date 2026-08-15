package com.ecommerce.commerceapi.inventory.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record InventoryMovementResponse(
        Long id,
        LocalDate transactionDate,
        Instant createdAt,
        String sku,
        String productName,
        String movementType,
        int quantity,
        Integer previousStock,
        Integer newStock,
        String referenceType,
        String referenceId,
        String reason,
        String remarks,
        Long userId,
        String userName,
        BigDecimal unitCost) {
}

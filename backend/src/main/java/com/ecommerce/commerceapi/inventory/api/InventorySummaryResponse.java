package com.ecommerce.commerceapi.inventory.api;

import java.math.BigDecimal;

public record InventorySummaryResponse(
        Long variantId,
        String sku,
        String productName,
        String brandName,
        String categoryName,
        String color,
        String size,
        int currentStock,
        int lowStockThreshold,
        String status,
        BigDecimal unitCost,
        BigDecimal inventoryValue) {
}

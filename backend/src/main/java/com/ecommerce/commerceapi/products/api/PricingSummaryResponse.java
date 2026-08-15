package com.ecommerce.commerceapi.products.api;

import java.math.BigDecimal;

public record PricingSummaryResponse(
        Long variantId,
        String sku,
        String productName,
        String brandName,
        String categoryName,
        String colorName,
        String sizeName,
        boolean active,
        BigDecimal unitCost,
        BigDecimal sellingPrice,
        BigDecimal grossProfit,
        BigDecimal marginPercent,
        int currentStock,
        BigDecimal inventoryValue) {
}

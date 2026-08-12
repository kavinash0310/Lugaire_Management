package com.ecommerce.commerceapi.purchases.api;

import java.math.BigDecimal;

public record PurchaseItemResponse(Long id, Long variantId, String productName, String color, String size, String sku,
                                   int quantity, BigDecimal unitCost, BigDecimal tax, BigDecimal lineTotal) {}

package com.ecommerce.commerceapi.orders.api;

import java.math.BigDecimal;

public record OrderItemResponse(Long id, Long variantId, String productName, String color, String size, String sku,
                                int quantity, int returnedQuantity, int remainingReturnableQuantity,
                                BigDecimal sellingPrice, BigDecimal lineTotal) {}

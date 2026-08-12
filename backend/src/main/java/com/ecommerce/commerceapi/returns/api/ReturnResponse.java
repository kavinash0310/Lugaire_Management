package com.ecommerce.commerceapi.returns.api;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReturnResponse(Long id, String orderId, String orderPlatform, LocalDate orderDate, String orderStatus,
                             Long orderItemId, String productName, String color, String size, String sku, String type,
                             String reason, int quantity, BigDecimal productCost, BigDecimal shippingLoss,
                             BigDecimal otherLoss, BigDecimal totalLoss, boolean resellable, String status,
                             LocalDate returnDate, LocalDate receivedDate, String remarks, boolean inventoryRestored) {
}

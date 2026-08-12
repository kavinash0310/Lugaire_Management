package com.ecommerce.commerceapi.orders.api;

import java.time.LocalDate;

public record OrderResponse(
        Long id,
        String orderId,
        String platform,
        LocalDate orderDate,
        String orderStatus,
        String paymentStatus,
        boolean inventoryDeducted) {}

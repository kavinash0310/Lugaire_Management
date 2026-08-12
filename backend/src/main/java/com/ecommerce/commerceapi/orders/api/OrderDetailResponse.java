package com.ecommerce.commerceapi.orders.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OrderDetailResponse(
        Long id,
        String orderId,
        String platform,
        LocalDate orderDate,
        String orderStatus,
        String paymentStatus,
        String customerName,
        String customerPhone,
        String shippingAddress,
        String city,
        String state,
        String pincode,
        BigDecimal totalOrderValue,
        BigDecimal commission,
        BigDecimal shippingCharge,
        BigDecimal otherCharges,
        BigDecimal netAmount,
        List<OrderItemResponse> items) {
}
